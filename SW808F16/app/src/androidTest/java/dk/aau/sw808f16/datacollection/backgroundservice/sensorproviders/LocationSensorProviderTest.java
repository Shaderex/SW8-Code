package dk.aau.sw808f16.datacollection.backgroundservice.sensorproviders;

import android.content.Context;
import android.hardware.SensorManager;
import android.location.Location;
import android.test.ApplicationTestCase;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import dk.aau.sw808f16.datacollection.DataCollectionApplication;
import dk.aau.sw808f16.datacollection.snapshot.Sample;

public class LocationSensorProviderTest extends ApplicationTestCase<DataCollectionApplication> {

  private static final long sampleDuration = 0; // In milliseconds
  private static final int measurementFrequency = 0; // In microseconds

  // GPS will always return exactly one GPS
  private static final int expectedSize = 1;

  public LocationSensorProviderTest() {
    super(DataCollectionApplication.class);
  }

  public void testGpsSensorProviderData() throws Exception {
    final ExecutorService sensorThreadPool = Executors.newFixedThreadPool(1);
    final SensorManager sensorManager = (SensorManager) getContext().getSystemService(Context.SENSOR_SERVICE);
    final LocationSensorProvider locationSensorProvider = new LocationSensorProvider(getContext(), sensorThreadPool, sensorManager);

    final Sample sample1 = locationSensorProvider.retrieveSampleForDuration(sampleDuration, measurementFrequency);
    final Sample sample2 = locationSensorProvider.retrieveSampleForDuration(sampleDuration, measurementFrequency);

    assertNotNull("Sensor data is null", sample1);
    assertFalse("Sensor data is empty", sample1.getMeasurements().isEmpty());

    assertNotNull("Sensor data (second measure) is null", sample2);
    assertFalse("Sensor data (second measure) is empty", sample2.getMeasurements().isEmpty());

    assertTrue("The amount of data and sampling period do not match, they are not exactly one",
        sample1.getMeasurements().size() == expectedSize);
    assertTrue("The amount of data and sampling period do not match, they are not exactly one",
        sample2.getMeasurements().size() == expectedSize);

    for (final Object measurement : sample1.getMeasurements()) {
      assertTrue("Measurement in sample1 is not of type Location", measurement instanceof Location);
    }

    for (final Object measurement : sample1.getMeasurements()) {
      assertTrue("Measurement in sample2 is not of type Location", measurement instanceof Location);
    }
  }
}
