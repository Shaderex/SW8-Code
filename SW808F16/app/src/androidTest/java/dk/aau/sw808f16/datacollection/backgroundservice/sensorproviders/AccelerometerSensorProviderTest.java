package dk.aau.sw808f16.datacollection.backgroundservice.sensorproviders;

import android.hardware.Sensor;

import java.util.concurrent.ExecutionException;

import dk.aau.sw808f16.datacollection.snapshot.measurement.FloatTripleMeasurement;

public class AccelerometerSensorProviderTest extends SensorProviderApplicationTestCase {

  @Override
  protected SensorProvider getSensorProvider() {
    return new AccelerometerSensorProvider(getContext(), sensorThreadPool, sensorManager);
  }

  @Override
  protected void validateMeasurement(final Object measurement, final String sampleIdentifier) {
    if (!(measurement instanceof FloatTripleMeasurement)) {
      assertEquals("[" + sampleIdentifier + "] Measurement in sample is of wrong type.",
          FloatTripleMeasurement.class, measurement.getClass());
    }

    @SuppressWarnings("ConstantConditions")
    final FloatTripleMeasurement accelerometerValues = (FloatTripleMeasurement) measurement;

    final Sensor sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    final float maxValue = sensor.getMaximumRange();
    final float minValue = -sensor.getMaximumRange();

    // Check the value boundaries of the sample
    assertTrue("[" + sampleIdentifier + "] first value of measurement must be between " + minValue + " and " + maxValue,
        accelerometerValues.getFirstValue() <= maxValue && accelerometerValues.getFirstValue() >= minValue);
    assertTrue("[" + sampleIdentifier + "] second value of measurement must be between " + minValue + " and " + maxValue,
        accelerometerValues.getSecondValue() <= maxValue && accelerometerValues.getSecondValue() >= minValue);
    assertTrue("[" + sampleIdentifier + "] third value of measurement must be between " + minValue + " and " + maxValue,
        accelerometerValues.getThirdValue() <= maxValue && accelerometerValues.getThirdValue() >= minValue);
  }

  @Override
  public void testGetSamples() throws ExecutionException, InterruptedException {
    super.testGetSamples();
  }
}
