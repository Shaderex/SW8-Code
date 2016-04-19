package dk.aau.sw808f16.datacollection.gcm;

import android.test.ApplicationTestCase;

import dk.aau.sw808f16.datacollection.DataCollectionApplication;

public class RegistrationIntentServiceTest extends ApplicationTestCase<DataCollectionApplication> {
  public RegistrationIntentServiceTest() {
    super(DataCollectionApplication.class);
  }

  public void testFindWifiSsid() {
    final RegistrationIntentService service = new RegistrationIntentService();
    assertNotNull(service.findWifiSsid(getContext()));
  }

}
