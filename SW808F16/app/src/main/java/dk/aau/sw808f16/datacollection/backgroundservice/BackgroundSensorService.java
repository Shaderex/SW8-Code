package dk.aau.sw808f16.datacollection.backgroundservice;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.hardware.SensorManager;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import com.goebl.david.Request;
import com.goebl.david.Response;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;

import org.json.JSONObject;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import dk.aau.sw808f16.datacollection.DataCollectionApplication;
import dk.aau.sw808f16.datacollection.R;
import dk.aau.sw808f16.datacollection.backgroundservice.sensorproviders.AccelerometerSensorProvider;
import dk.aau.sw808f16.datacollection.backgroundservice.sensorproviders.AccelerometerSensorProviderBand;
import dk.aau.sw808f16.datacollection.backgroundservice.sensorproviders.AmbientLightSensorProvider;
import dk.aau.sw808f16.datacollection.backgroundservice.sensorproviders.BarometerSensorProvider;
import dk.aau.sw808f16.datacollection.backgroundservice.sensorproviders.CompassSensorProvider;
import dk.aau.sw808f16.datacollection.backgroundservice.sensorproviders.GalvanicSkinResponseSensorProviderBand;
import dk.aau.sw808f16.datacollection.backgroundservice.sensorproviders.GyroscopeSensorProvider;
import dk.aau.sw808f16.datacollection.backgroundservice.sensorproviders.HeartbeatSensorProviderBand;
import dk.aau.sw808f16.datacollection.backgroundservice.sensorproviders.LocationSensorProvider;
import dk.aau.sw808f16.datacollection.backgroundservice.sensorproviders.ProximitySensorProvider;
import dk.aau.sw808f16.datacollection.backgroundservice.sensorproviders.SensorProvider;
import dk.aau.sw808f16.datacollection.backgroundservice.sensorproviders.UltraVioletSensorProviderBand;
import dk.aau.sw808f16.datacollection.backgroundservice.sensorproviders.WifiSensorProvider;
import dk.aau.sw808f16.datacollection.campaign.AsyncHttpCampaignJoinTask;
import dk.aau.sw808f16.datacollection.campaign.Campaign;
import dk.aau.sw808f16.datacollection.questionaire.models.Questionnaire;
import dk.aau.sw808f16.datacollection.snapshot.Snapshot;
import dk.aau.sw808f16.datacollection.webutil.AsyncHttpWebbTask;
import dk.aau.sw808f16.datacollection.webutil.RequestHostResolver;
import io.realm.Realm;
import io.realm.RealmConfiguration;

public final class BackgroundSensorService extends IntentService {

  public static final int NOTIFY_NEW_CAMPAIGN = 1337;
  public static final int NOTIFY_QUESTIONNAIRE_COMPLETED = 31337;

  // Messaging acknowledgement constants
  public static final int SERVICE_ACK_OK = 42;
  public static final int SERVICE_ACK_BUSY = 43;

  public static final String NOTIFY_QUESTIONNAIRE_COMPLETED_TIMESTAMP = "NOTIFY_QUESTIONNAIRE_COMPLETED_TIMESTAMP";
  public static final String NOTIFY_QUESTIONNAIRE_COMPLETED_QUESTIONNAIRE = "NOTIFY_QUESTIONNAIRE_COMPLETED_QUESTIONNAIRE";
  public static final String NOTIFY_QUESTIONNAIRE_COMPLETED_CAMPAIGN_ID = "NOTIFY_QUESTIONNAIRE_COMPLETED_CAMPAIGN_ID";

  private static final String REALM_NAME = DataCollectionApplication.TAG + ".realm";
  private static final long SYNCHRONIZATION_INTERVAL = 10000;
  private byte[] encryptionKey = null;

  private ServiceHandler serviceHandler;
  private Messenger messenger;

  private ExecutorService sensorThreadPool;

  private AccelerometerSensorProvider accelerometerSensorProvider;
  private AmbientLightSensorProvider ambientLightSensorProvider;
  private BarometerSensorProvider barometerSensorProvider;
  private CompassSensorProvider compassSensorProvider;
  private GyroscopeSensorProvider gyroscopeSensorProvider;
  private ProximitySensorProvider proximitySensorProvider;
  private WifiSensorProvider wifiSensorProvider;
  private LocationSensorProvider locationSensorProvider;

  // Band providers
  private UltraVioletSensorProviderBand ultraVioletSensorProviderBand;
  private GalvanicSkinResponseSensorProviderBand galvanicSkinResponseSensorProviderBand;
  private AccelerometerSensorProviderBand accelerometerSensorProviderBand;
  private HeartbeatSensorProviderBand heartbeatSensorProviderBand;

  private SnapshotTimer snapshotTimer;
  private SynchronizationTimer synchronizationTimer;

  private ConnectivityManager.OnNetworkActiveListener networkActiveListener;
  private ConnectivityManager connectivityManager;

  private HandlerThread handlerThread;

  public BackgroundSensorService() {
    super("BackgroundSensorService");
  }

  @Override
  public void onCreate() {
    super.onCreate();

    // The number of threads in the pool should correspond to the number of SensorProvider instances
    // this service maintains
    // Dynamically (Reflection) counts the number of SensorProvider instances this service maintains
    final int numberOfSensorProviders = getNumberOfSensorProviders();
    // Create a handlerThread pool to be shared by all sensor providers
    sensorThreadPool = Executors.newFixedThreadPool(numberOfSensorProviders);

    connectivityManager = (ConnectivityManager) getApplicationContext().getSystemService(CONNECTIVITY_SERVICE);

    final SensorManager sensorManager = (SensorManager) getApplicationContext().getSystemService(SENSOR_SERVICE);

    // Initialize SensorProvider instances with the shared handlerThread pool
    accelerometerSensorProvider = new AccelerometerSensorProvider(BackgroundSensorService.this, sensorThreadPool, sensorManager);
    ambientLightSensorProvider = new AmbientLightSensorProvider(BackgroundSensorService.this, sensorThreadPool, sensorManager);
    barometerSensorProvider = new BarometerSensorProvider(BackgroundSensorService.this, sensorThreadPool, sensorManager);
    compassSensorProvider = new CompassSensorProvider(BackgroundSensorService.this, sensorThreadPool, sensorManager);
    gyroscopeSensorProvider = new GyroscopeSensorProvider(BackgroundSensorService.this, sensorThreadPool, sensorManager);
    proximitySensorProvider = new ProximitySensorProvider(BackgroundSensorService.this, sensorThreadPool, sensorManager);
    wifiSensorProvider = new WifiSensorProvider(BackgroundSensorService.this, sensorThreadPool, sensorManager);
    locationSensorProvider = new LocationSensorProvider(BackgroundSensorService.this, sensorThreadPool, sensorManager);

    ultraVioletSensorProviderBand = new UltraVioletSensorProviderBand(BackgroundSensorService.this, sensorThreadPool, sensorManager);
    galvanicSkinResponseSensorProviderBand = new GalvanicSkinResponseSensorProviderBand(BackgroundSensorService.this,
        sensorThreadPool,
        sensorManager);
    accelerometerSensorProviderBand = new AccelerometerSensorProviderBand(BackgroundSensorService.this, sensorThreadPool, sensorManager);
    heartbeatSensorProviderBand = new HeartbeatSensorProviderBand(BackgroundSensorService.this, sensorThreadPool, sensorManager);

    snapshotTimer = new SnapshotTimer(BackgroundSensorService.this, getSensorProviders());
    synchronizationTimer = new SynchronizationTimer(BackgroundSensorService.this, SYNCHRONIZATION_INTERVAL);

    // Start up the handlerThread running the service.  Note that we create a
    // separate handlerThread because the service normally runs in the process's
    // main handlerThread, which we don't want to block.  We also make it
    // background priority so CPU-intensive work will not disrupt our UI.
    handlerThread = new HandlerThread("ServiceStartArguments",
        android.os.Process.THREAD_PRIORITY_BACKGROUND);
    handlerThread.start();

    // Get the HandlerThread's Looper and use it for our Handler
    serviceHandler = new ServiceHandler(handlerThread.getLooper());
    messenger = new Messenger(serviceHandler);

    Log.d("BackgroundSensorService", "BackgroundSensorService onCreate() called");

    serviceHandler.post(new RealmSetupRunnable());
  }

  private void retryRealmSetupOnNetworkChanged() {
    networkActiveListener = new ConnectivityManager.OnNetworkActiveListener() {
      @Override
      public void onNetworkActive() {

        // Wait for the network to truly be ready (can be delayed - at least on AAU)
        try {
          Thread.sleep(1000);
        } catch (InterruptedException exception) {
          exception.printStackTrace();
        }

        serviceHandler.post(new RealmSetupRunnable());
        connectivityManager.removeDefaultNetworkActiveListener(networkActiveListener);
      }
    };

    connectivityManager.addDefaultNetworkActiveListener(networkActiveListener);
  }

  public static String bytesToHex(byte[] in) {
    final StringBuilder builder = new StringBuilder();
    for (byte b : in) {
      builder.append(String.format("%02x", b));
    }
    return builder.toString();
  }

  private void setupRealmAndStartTimers() {

    final RealmConfiguration realmConfiguration = new RealmConfiguration.Builder(BackgroundSensorService.this)
        .name(REALM_NAME)
        .encryptionKey(encryptionKey)
        .build();

    Realm.setDefaultConfiguration(realmConfiguration);

    // Check if device is subscribed to a campaign and then continue that campaign
    final Realm realm = Realm.getDefaultInstance();
    final Campaign campaign = realm.where(Campaign.class).findFirst();

    if (campaign != null) {
      serviceHandler.post(new Runnable() {
        @Override
        public void run() {
          snapshotTimer.start();
        }
      });
    }

    realm.close();

    // Start the sync of snapshots
    serviceHandler.post(new Runnable() {
      @Override
      public void run() {
        synchronizationTimer.start();
      }
    });
  }

  private final class ServiceHandler extends Handler {


    public ServiceHandler(final Looper looper) {
      super(looper);
    }

    @Override
    public void handleMessage(final Message msg) {

      Log.d("BackgroundSensorService", "handleMessage called");
      // Check if realm has been properly setup by checking the encryption key
      if (encryptionKey == null) {
        final Message busy = Message.obtain(null, SERVICE_ACK_BUSY);
        try {
          msg.replyTo.send(busy);
        } catch (RemoteException exception) {
          exception.printStackTrace();
        }
        return;
      }


      final Bundle data = msg.getData();
      switch (msg.what) {
        case NOTIFY_NEW_CAMPAIGN: {

          final long campaignId = data.getLong(NOTIFY_QUESTIONNAIRE_COMPLETED_CAMPAIGN_ID);
          notifyNewCampaign(campaignId, msg);
          return;
        }
        case NOTIFY_QUESTIONNAIRE_COMPLETED: {

          final long timestamp = data.getLong(NOTIFY_QUESTIONNAIRE_COMPLETED_TIMESTAMP);
          final Questionnaire questionnaire = data.getParcelable(NOTIFY_QUESTIONNAIRE_COMPLETED_QUESTIONNAIRE);
          notifyQuestionnaireCompleted(timestamp, questionnaire);
          return;
        }
        default: {
          return;
        }
      }
    }
  }

  public void notifyNewCampaign(final long campaignId, final Message originalMessage) {

    final Messenger replyMessenger = originalMessage.replyTo;

    final AsyncHttpCampaignJoinTask joinCampaignTask = new AsyncHttpCampaignJoinTask(this, campaignId) {
      @Override
      public void onResponseCodeMatching(final Response<JSONObject> response) {
        super.onResponseCodeMatching(response);


        final Message ok = Message.obtain(null, SERVICE_ACK_OK);

        try {
          replyMessenger.send(ok);
        } catch (RemoteException exception) {
          exception.printStackTrace();
        }

        snapshotTimer.stop();
        snapshotTimer.start();
      }
    };
    joinCampaignTask.execute();
  }

  public void notifyQuestionnaireCompleted(final long snapshotTimeStamp, Questionnaire questionnaire) {

    Realm realm = null;

    try {
      realm = Realm.getDefaultInstance();

      final Snapshot snapshot = realm.where(Snapshot.class).equalTo("timestamp", snapshotTimeStamp).findFirst();

      if (snapshot != null) {
        realm.beginTransaction();
        questionnaire = realm.copyToRealm(questionnaire);
        realm.commitTransaction();

        realm.beginTransaction();
        snapshot.setQuestionnaire(questionnaire);
        realm.copyToRealmOrUpdate(snapshot);
        realm.commitTransaction();
      }
    } finally {
      if (realm != null) {
        realm.close();
      }
    }
  }

  @Override
  protected void onHandleIntent(final Intent intent) {

  }

  @Override
  public IBinder onBind(final Intent intent) {
    Log.d("BackgroundSensorService", "BackgroundSensorService onBind() called");
    return messenger.getBinder();
  }

  @Override
  public int onStartCommand(final Intent intent, final int flags, final int startId) {
    Log.d("BackgroundSensorService", "BackgroundSensorService onStartCommand() called");
    return START_STICKY;
  }

  @Override
  public void onDestroy() {
    Log.d("BackgroundSensorService", "BackgroundSensorService onDestroy() called");
    snapshotTimer.stop();
    synchronizationTimer.stop();
    sensorThreadPool.shutdown();
    handlerThread.quitSafely();
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

  private List<SensorProvider> getSensorProviders() {

    final List<SensorProvider> sensorProvides = new ArrayList<>();

    for (final Field field : BackgroundSensorService.class.getDeclaredFields()) {

      final Class type = field.getType();

      if (SensorProvider.class.isAssignableFrom(type)) {
        try {
          sensorProvides.add((SensorProvider) field.get(this));
        } catch (IllegalAccessException exception) {
          exception.printStackTrace();
        }
      }
    }

    return sensorProvides;
  }

  private class RealmSetupRunnable implements Runnable {
    @Override
    public void run() {

      final String campaignListResourcePath = RequestHostResolver.resolveHostForRequest(BackgroundSensorService.this, "/key");
      final WeakReference<Context> weakContextReference = new WeakReference<Context>(BackgroundSensorService.this.getBaseContext());

      final AsyncHttpWebbTask<String> keyTask = new AsyncHttpWebbTask<String>(AsyncHttpWebbTask.Method.GET,
          campaignListResourcePath,
          HttpURLConnection.HTTP_OK) {
        @Override
        protected Response<String> sendRequest(Request request) {
          final Context context = weakContextReference.get();

          if (context != null) {
            try {
              final InstanceID instanceId = InstanceID.getInstance(context);
              final String token = instanceId.getToken(
                  context.getString(R.string.defaultSenderID),
                  GoogleCloudMessaging.INSTANCE_ID_SCOPE,
                  null
              );

              return request.param("device_id", token).retry(3, false).asString();

            } catch (IOException exception) {
              exception.printStackTrace();
              return null;
            }
          }
          return null;
        }

        @Override
        public void onResponseCodeMatching(final Response<String> response) {
          final String encryptStr = response.getBody();
          final int len = encryptStr.length();
          final byte[] data = new byte[len / 2];
          for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(encryptStr.charAt(i), 16) << 4)
                + Character.digit(encryptStr.charAt(i + 1), 16));
          }
          encryptionKey = data;

          // Sets up the realm configuration and start collection of snapshots
          setupRealmAndStartTimers();
        }

        @Override
        public void onResponseCodeNotMatching(Response<String> response) {
          Log.d("BackgroundSensorService", "onResponseCodeNotMatching called when requesting encryption key");
          retryRealmSetupOnNetworkChanged();
        }

        @Override
        public void onConnectionFailure() {
          Log.d("BackgroundSensorService", "onConnectionFailure called when requesting encryption key");
          retryRealmSetupOnNetworkChanged();
        }
      };

      keyTask.execute();
    }
  }

}
