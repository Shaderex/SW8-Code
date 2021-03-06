package dk.aau.sw808f16.datacollection.backgroundservice.sensors;

import android.app.Application;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.test.ApplicationTestCase;
import android.util.Log;

import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;

@SuppressWarnings("unused")
public class AccelerometerSensorTest extends ApplicationTestCase<Application> {

  // Increase this to increase the amount of time logging
  private static final int logTime = 0;

  public AccelerometerSensorTest() {
    super(Application.class);
  }

  final List<float[]> data = new LinkedList<>();

  public void testAccelerometer() {

    final long now = System.currentTimeMillis();
    final long whenToStop = now + logTime;
    final CountDownLatch latch = new CountDownLatch(1);

    final Timer timer = new Timer();

    final SensorManager sensorManager = (SensorManager) getContext().getSystemService(Context.SENSOR_SERVICE);
    final Sensor accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

    final SensorEventListener listener = new SensorEventListener() {
      @Override
      public void onSensorChanged(final SensorEvent event) {

        final float xAcceleration = event.values[0];
        final float yAcceleration = event.values[1];
        final float zAcceleration = event.values[2];

        Log.i("Acceleration ", xAcceleration + " ms^2 / " + accelerometerSensor.getMaximumRange() + " ms^2");
        Log.i("Acceleration ", yAcceleration + " ms^2 / " + accelerometerSensor.getMaximumRange() + " ms^2");
        Log.i("Acceleration ", zAcceleration + " ms^2 / " + accelerometerSensor.getMaximumRange() + " ms^2");

        data.add(event.values);
      }

      @Override
      public void onAccuracyChanged(final Sensor sensor, final int accuracy) {
      }
    };

    sensorManager.registerListener(listener, accelerometerSensor, SensorManager.SENSOR_DELAY_FASTEST);

    final TimerTask stopTask = new TimerTask() {
      @Override
      public void run() {
        latch.countDown();
      }
    };

    timer.schedule(stopTask, whenToStop - now);

    try {
      latch.await();
    } catch (InterruptedException exception) {
      exception.printStackTrace();
    }

    timer.cancel();

    sensorManager.unregisterListener(listener);
  }

  @Override
  protected void tearDown() throws Exception {

    Log.i("Accelerometer data: ", data.size() * 4 * 3 + " Bytes");

    super.tearDown();
  }
}

