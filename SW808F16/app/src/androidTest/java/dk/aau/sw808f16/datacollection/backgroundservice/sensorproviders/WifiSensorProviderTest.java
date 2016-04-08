package dk.aau.sw808f16.datacollection.backgroundservice.sensorproviders;

import java.util.concurrent.ExecutionException;

import dk.aau.sw808f16.datacollection.snapshot.measurement.WifiMeasurement;

public class WifiSensorProviderTest extends SensorProviderApplicationTestCase {
  @Override
  protected SensorProvider getSensorProvider() {
    return new WifiSensorProvider(getContext(), sensorThreadPool, sensorManager);
  }

  @Override
  protected void validateMeasurement(Object measurement, String sampleIdentifier) {
    if (!(measurement instanceof WifiMeasurement)) {
      assertEquals("[" + sampleIdentifier + "] measurement is of wrong type.", WifiMeasurement.class, measurement.getClass());
    }
  }

  @Override
  public void testGetSample() throws ExecutionException, InterruptedException, ClassCastException {
    super.testGetSample();
  }

  @Override
  public void testGetSamples() throws ExecutionException, InterruptedException {
    super.testGetSamples();
  }
}
