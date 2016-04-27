package dk.aau.sw808f16.datacollection.backgroundservice;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.SensorManager;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import com.goebl.david.Request;
import com.goebl.david.Response;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;

import org.json.JSONException;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import dk.aau.sw808f16.datacollection.DataCollectionApplication;
import dk.aau.sw808f16.datacollection.R;
import dk.aau.sw808f16.datacollection.SensorType;
import dk.aau.sw808f16.datacollection.backgroundservice.sensorproviders.AccelerometerSensorProvider;
import dk.aau.sw808f16.datacollection.backgroundservice.sensorproviders.SensorProvider;
import dk.aau.sw808f16.datacollection.campaign.Campaign;
import dk.aau.sw808f16.datacollection.questionaire.models.Question;
import dk.aau.sw808f16.datacollection.questionaire.models.Questionnaire;
import dk.aau.sw808f16.datacollection.snapshot.Sample;
import dk.aau.sw808f16.datacollection.snapshot.Snapshot;
import dk.aau.sw808f16.datacollection.webutil.AsyncHttpWebbTask;
import dk.aau.sw808f16.datacollection.webutil.RequestHostResolver;
import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.RealmObject;
import io.realm.RealmResults;

public final class BackgroundSensorService extends Service {
  public static final String SNAPSHOT_REALM_NAME = "snapshot.realm";
  private static final String REALM_NAME = DataCollectionApplication.TAG + ".realm";
  private ServiceHandler serviceHandler;

  private final ExecutorService sensorThreadPool;

  private AccelerometerSensorProvider accelerometerSensorProvider;

  public BackgroundSensorService() {
    // The number of threads in the pool should correspond to the number of SensorProvider instances
    // this service maintains
    // Dynamically (Reflection) counts the number of SensorProvider instances this service maintains
    final int numberOfSensorProviders = getNumberOfSensorProviders();
    // Create a thread pool to be shared by all sensor providers
    sensorThreadPool = Executors.newFixedThreadPool(numberOfSensorProviders);
  }

  private final class ServiceHandler extends Handler {

    public ServiceHandler(final Looper looper) {
      super(looper);
    }

    @Override
    public void handleMessage(final Message msg) {


    }
  }

  public class LocalBinder extends Binder {
    public BackgroundSensorService getService() {
      return BackgroundSensorService.this;
    }
  }

  @Override
  public void onCreate() {
    super.onCreate();

    final RealmConfiguration realmConfiguration = new RealmConfiguration.Builder(BackgroundSensorService.this)
        .name(REALM_NAME)
        .encryptionKey(getSecretKey())
        .build();

    Realm.setDefaultConfiguration(realmConfiguration);

    final SensorManager sensorManager = (SensorManager) getApplicationContext().getSystemService(SENSOR_SERVICE);

    // Initialize SensorProvider instances with the shared thread pool
    accelerometerSensorProvider = new AccelerometerSensorProvider(this, sensorThreadPool, sensorManager);

    // Start up the thread running the service.  Note that we create a
    // separate thread because the service normally runs in the process's
    // main thread, which we don't want to block.  We also make it
    // background priority so CPU-intensive work will not disrupt our UI.
    final HandlerThread thread = new HandlerThread("ServiceStartArguments",
        android.os.Process.THREAD_PRIORITY_BACKGROUND);
    thread.start();

    // Get the HandlerThread's Looper and use it for our Handler
    final Looper serviceLooper = thread.getLooper();
    serviceHandler = new ServiceHandler(serviceLooper);
  }

  @Override
  public int onStartCommand(final Intent intent, final int flags, final int startId) {

    Toast.makeText(this, "service starting", Toast.LENGTH_SHORT).show();

    // Check if device is subscribed to a campaign and then continue that campaign
    Realm realm = null;

    try {
      realm = Realm.getDefaultInstance();

      final Campaign campaign = realm.where(Campaign.class).findFirst();

      if (campaign != null) {
        activateCampaign(campaign);
      }
    } finally {
      if (realm != null) {
        realm.close();
      }
    }


    final Runnable addSnapshotRunnable = new Runnable() {
      @Override
      public void run() {
        List<Sample> accelerometerSamples;

        // Find the campaign from the database
        Realm realm = Realm.getDefaultInstance();
        Campaign campaign = realm.where(Campaign.class).findFirst();

        if (campaign == null) {
          realm.close();
          return;
        }

        try {

          final Snapshot snapshot = Snapshot.Create();

          accelerometerSamples = accelerometerSensorProvider.retrieveSamplesForDuration(2 * 60 * 1000, 1000, 500, 500).get();
          snapshot.addSamples(SensorType.ACCELEROMETER, accelerometerSamples);
          Questionnaire questionnaire = new Questionnaire(campaign.getQuestionnaire());

          for (Question question : questionnaire.getQuestions()) {
            questionnaire.getNextQuestion().setAnswer(true);
          }

          snapshot.setQuestionnaire(questionnaire);

          realm.beginTransaction();
          // Attach the newly created snapshot so that it will also be saved
          campaign.addSnapshot(snapshot);

          realm.copyToRealmOrUpdate(campaign);

          realm.commitTransaction();
          realm.close();

        } catch (InterruptedException | ExecutionException exception) {
          exception.printStackTrace();
        }
      }
    };

    // Start collection of data every x minutes
    new Timer().scheduleAtFixedRate(new TimerTask() {
      @Override
      public void run() {
        final Thread thread = new Thread(addSnapshotRunnable);
        thread.start();
      }
    }, 0, 5 * 60 * 1000);

    // Send the campaign to the server every x minutes
    new Timer().scheduleAtFixedRate(new TimerTask() {
      @Override
      public void run() {
        // Check if we have access to wifi. If not, don't try to synchronize
        WifiManager wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        if (!wifi.isWifiEnabled()) {
          Log.d("CampaignSyncLog", "Unable to upload without network");
          return; // Take no further actions
        }
        final Realm realm = Realm.getDefaultInstance();
        RealmResults<Campaign> results = realm.where(Campaign.class).findAll();


        if (results.size() == 0) {
          Log.d("CampaignSyncLog", "There are no campaigns to be uploaded");
        }

        for (int i = 0; i < results.size(); i++) {
          final Campaign campaign = results.get(i);
          final String requestUrl = RequestHostResolver.resolveHostForRequest(BackgroundSensorService.this,
              "/campaigns/" + campaign.getIdentifier() + "/snapshots");

          try {
            final String campaignString = campaign.toJsonObject().toString();
            final int campaignIdentifer = campaign.getIdentifier();

            // Send the campaign to the server
            final AsyncHttpWebbTask<String> task = new AsyncHttpWebbTask<String>(AsyncHttpWebbTask.Method.POST, requestUrl, 200) {
              @Override
              protected Response<String> sendRequest(Request webb) {
                Context context = BackgroundSensorService.this;
                try {
                  final InstanceID instanceId = InstanceID.getInstance(context);
                  final String token = instanceId.getToken(
                      context.getString(R.string.defaultSenderID),
                      GoogleCloudMessaging.INSTANCE_ID_SCOPE,
                      null
                  );
                  final Response<String> jsonString =
                      webb.param("snapshots", campaignString)
                          .param("device_id", token)
                          .asString();

                  Log.d("Service-status", campaignString);
                  return jsonString;
                } catch (IOException e) {
                  e.printStackTrace();
                  return null;
                }
              }

              @Override
              public void onResponseCodeMatching(Response<String> response) {
                Log.d("CampaignSyncLog", "All campaigns were uploaded");
                final Realm realm = Realm.getDefaultInstance();
                realm.beginTransaction();
                RealmResults<Campaign> campaigns = realm.where(Campaign.class).equalTo("identifier", campaignIdentifer).findAll();

                Campaign campaign = campaigns.get(0);

                for (Snapshot snapshot : campaign.getSnapshots()) {
                  for (RealmObject obj : snapshot.children()) {
                    obj.removeFromRealm();
                  }
                }

                realm.commitTransaction();
                realm.close();
              }

              @Override
              public void onResponseCodeNotMatching(Response<String> response) {
              }

              @Override
              public void onConnectionFailure() {
              }
            };
            task.execute();

          } catch (JSONException exception) {
            exception.printStackTrace();
          }
        }
        realm.close();
      }
    }, 0, 5 * 1000);

    // For each start request, send a message to start a job and deliver the
    // start ID so we know which request we're stopping when we finish the job
    final Message msg = serviceHandler.obtainMessage();
    msg.arg1 = startId;
    serviceHandler.sendMessage(msg);

    // If we get killed, after returning from here, restart
    return START_STICKY;
  }

  private byte[] getSecretKey() {
    // TODO Use the correct encryption key provided by the server
    return new byte[] {-92, -42, -86, 62, 15, 2, -92, 79,
        31, 46, 76, 81, -25, -39, 50, 77,
        30, -2, -54, 48, 107, -115, 56, 125,
        -119, 90, 11, -108, -120, -103, -38, 126,
        -92, 120, 15, 100, -74, 41, -108, -70,
        -95, 83, -96, 64, -70, -98, -73, 89,
        -62, 51, -25, 37, 119, 53, -59, 4,
        0, -74, 47, 13, -124, 0, 117, 9};
  }

  @Override
  public IBinder onBind(final Intent intent) {
    return new LocalBinder();
  }

  @Override
  public void onDestroy() {
    Toast.makeText(this, "service done", Toast.LENGTH_SHORT).show();
  }

  private static int getNumberOfSensorProviders() {

    int sensorProviderCount = 0;

    for (final Field field : BackgroundSensorService.class.getDeclaredFields()) {

      final Class type = field.getType();

      if (SensorProvider.class.isAssignableFrom(type)) {
        sensorProviderCount++;
      }
    }

    return sensorProviderCount;
  }

  private final HashSet<Campaign> activeCampaigns = new HashSet<>();

  private void activateCampaign(final Campaign campaign)
  {
    serviceHandler.post(new Runnable() {
      @Override
      public void run() {



      }
    });
  }

  private void cancelCampaign(final Campaign campaign)
  {


  }




}
