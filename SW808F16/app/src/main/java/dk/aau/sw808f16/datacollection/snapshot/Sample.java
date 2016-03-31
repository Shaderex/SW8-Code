package dk.aau.sw808f16.datacollection.snapshot;

import java.util.ArrayList;
import java.util.List;

import io.realm.RealmList;
import io.realm.RealmObject;
import io.realm.annotations.Ignore;


public class Sample extends RealmObject {

  @Ignore
  private Class clazz = null;

  private RealmList<FloatTripleMeasurement> floatTripleMeasurements = new RealmList<>();

  private RealmList<FloatMeasurement> floatMeasurements = new RealmList<>();

  public Sample() {
  }

  public Sample(final Object initialMeasurement) {
    addMeasurement(initialMeasurement);
  }

  public Sample(final List<?> initialMeasurements) {
    for (Object o : initialMeasurements) {
      addMeasurement(o);
    }
  }

  public void addMeasurement(final Object measurement) {
    if (clazz == null) {
      clazz = measurement.getClass();
    } else if (!clazz.equals(measurement.getClass())) {
      throw new IllegalArgumentException("The sample contains measurements of type " + clazz.getName()
          + " you cannot add measurements of type " + measurement.getClass().getName());
    }

    if (measurement instanceof FloatTripleMeasurement) {
      floatTripleMeasurements.add((FloatTripleMeasurement) measurement);
    } else if (measurement instanceof FloatMeasurement) {
      floatMeasurements.add((FloatMeasurement) measurement);
    } else {
      throw new IllegalArgumentException("Type " + measurement.getClass().getName() + " is not a supported measurement type");
    }
  }

  public void addMeasurements(final List<?> measurements) {
    for (Object o : measurements) {
      addMeasurement(o);
    }
  }

  public List<?> getMeasurements() {
    List<Object> result = new ArrayList<>();

    // Concatenate the different lists into a single one (there should only be one list containing elements)
    result.addAll(floatTripleMeasurements);
    result.addAll(floatMeasurements);

    return result;
  }

  @Override
  public boolean equals(final Object object) {
    if (this == object) {
      return true;
    }

    if (object == null || !Sample.class.isAssignableFrom(object.getClass())) {
      return false;
    }

    final Sample that = (Sample) object;

    List<?> ourMeasurements = this.getMeasurements();
    List<?> theirMeasurements = that.getMeasurements();

    if (ourMeasurements.size() != theirMeasurements.size()) {
      return false;
    } else {
      for (int i = 0; i < ourMeasurements.size(); i++) {
        if (!ourMeasurements.get(i).equals(theirMeasurements.get(i))) {
          return false;
        }
      }
    }

    return true;
  }
}
