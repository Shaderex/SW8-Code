package dk.aau.sw808f16.datacollection.backgroundservice.sensorproviders;

import java.util.concurrent.ExecutionException;

import dk.aau.sw808f16.datacollection.snapshot.measurement.LocationMeasurement;

public class LocationSensorProviderTest extends SensorProviderApplicationTestCase {

  @Override
  protected SensorProvider getSensorProvider() {
    return new LocationSensorProvider(getContext(), sensorThreadPool, sensorManager);
  }

  @Override
  protected void validateMeasurement(final Object measurement, final String sampleIdentifier) {
    assertTrue("[" + sampleIdentifier + "] measurement is not of type Location",
        LocationMeasurement.class.isAssignableFrom(measurement.getClass()));
  }

  @Override
  public void testGetSamples() throws ExecutionException, InterruptedException {
    super.testGetSamples();
  }
}
