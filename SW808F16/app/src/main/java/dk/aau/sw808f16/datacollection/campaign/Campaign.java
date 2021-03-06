package dk.aau.sw808f16.datacollection.campaign;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import dk.aau.sw808f16.datacollection.SensorType;
import dk.aau.sw808f16.datacollection.questionaire.models.Question;
import dk.aau.sw808f16.datacollection.questionaire.models.Questionnaire;
import dk.aau.sw808f16.datacollection.snapshot.JsonObjectAble;
import dk.aau.sw808f16.datacollection.snapshot.Snapshot;
import io.realm.RealmList;
import io.realm.RealmObject;
import io.realm.annotations.Ignore;
import io.realm.annotations.PrimaryKey;

public class Campaign extends RealmObject implements JsonObjectAble {

  @PrimaryKey
  private int identifier;

  private RealmList<Snapshot> snapshots;
  private String name;
  private String description;
  private boolean isPrivate;
  private int snapshotLength;
  private int sampleDuration;
  private int sampleFrequency;
  private int measurementFrequency;
  private String sensorString;

  @Ignore
  private List<SensorType> sensors;

  @Ignore
  private QuestionnairePlacement questionnairePlacement;
  private int placementId;

  private Questionnaire questionnaire;
  private int campaignLength;

  public Campaign() {
    if (snapshots == null) {
      snapshots = new RealmList<>();
    }
  }

  public Campaign(final int identifier) {
    this();
    setIdentifier(identifier);
  }

  public Campaign(final JSONObject jsonObject) throws JSONException {
    this();

    this.setIdentifier(jsonObject.getInt("id"));
    this.setName(jsonObject.getString("name"));
    this.setDescription(jsonObject.getString("description"));
    this.setPrivate(jsonObject.getBoolean("is_private"));
    this.setSnapshotLength(jsonObject.getInt("snapshot_length"));
    this.setSampleDuration(jsonObject.getInt("sample_duration"));
    this.setSampleFrequency(jsonObject.getInt("sample_frequency"));
    this.setMeasurementFrequency(jsonObject.getInt("measurement_frequency"));
    this.setCampaignLength(jsonObject.getInt("campaign_length"));
    this.placementId = jsonObject.getInt("questionnaire_placement");

    JSONArray sensors = jsonObject.getJSONArray("sensors");

    ArrayList<SensorType> sensorTypes = new ArrayList<>();
    for (int i = 0; i < sensors.length(); i++) {
      SensorType type = SensorType.getSensorTypeById(sensors.getJSONObject(i).getInt("type"));
      sensorTypes.add(type);
    }

    this.setSensors(sensorTypes);

    JSONArray questionsArray = jsonObject.getJSONArray("questions");
    List<Question> questions = new ArrayList<>();

    for (int i = 0; i < questionsArray.length(); i++) {
      String question = questionsArray.getJSONObject(i).getString("question");
      long id = questionsArray.getJSONObject(i).getLong("id");
      questions.add(new Question(question, id));
    }

    this.setQuestionnaire(new Questionnaire(questions));
  }

  public void addSnapshot(final Snapshot snapshot) {
    snapshots.add(snapshot);
  }

  public List<Snapshot> getSnapshots() {
    return snapshots;
  }

  @Override
  public boolean equals(final Object object) {
    if (!(object instanceof Campaign)) {
      return false;
    }

    final Campaign that = (Campaign) object;

    boolean isSame = this.identifier == that.identifier
        && this.name != null ? this.name.equals(that.name) : that.name == null
        && this.description != null ? this.description.equals(that.description) : that.name == null
        && this.isPrivate == that.isPrivate
        && this.snapshotLength == that.snapshotLength
        && this.sampleDuration == that.sampleDuration
        && this.sampleFrequency == that.sampleFrequency
        && this.measurementFrequency == that.measurementFrequency
        && this.sensorString != null ? this.sensorString.equals(that.sensorString) : that.sensorString == null
        && this.getSensors().equals(that.getSensors())
        && this.questionnaire != null ? this.questionnaire.equals(that.questionnaire) : that.questionnaire == null;

    if (!isSame) {
      return false;
    } else {
      final List<Snapshot> ourSnapshots = this.getSnapshots();
      final List<Snapshot> theirSnapshots = that.getSnapshots();

      if (theirSnapshots == null) {
        return ourSnapshots == null;
      } else {
        return theirSnapshots.equals(ourSnapshots);
      }
    }
  }

  public int getIdentifier() {
    return identifier;
  }

  public void setIdentifier(final int identifier) {
    this.identifier = identifier;
  }

  @Override
  public JSONObject toJsonObject() throws JSONException {
    final JSONObject jsonObject = new JSONObject();

    final JSONArray snapshotArray = new JSONArray();
    for (Snapshot snapshot : getSnapshots()) {
      if (isSnapshotReady(snapshot)) {
        snapshotArray.put(snapshot.toJsonObject());
      }
    }
    jsonObject.put("snapshots", snapshotArray);

    return jsonObject;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public boolean isPrivate() {
    return isPrivate;
  }

  public void setPrivate(boolean isPrivate) {
    this.isPrivate = isPrivate;
  }

  public int getSnapshotLength() {
    return snapshotLength;
  }

  public void setSnapshotLength(int snapshotLength) {
    this.snapshotLength = snapshotLength;
  }

  public int getSampleDuration() {
    return sampleDuration;
  }

  public void setSampleDuration(int sampleDuration) {
    this.sampleDuration = sampleDuration;
  }

  public int getSampleFrequency() {
    return sampleFrequency;
  }

  public void setSampleFrequency(int sampleFrequency) {
    this.sampleFrequency = sampleFrequency;
  }

  public int getMeasurementFrequency() {
    return measurementFrequency;
  }

  public void setMeasurementFrequency(int measurementFrequency) {
    this.measurementFrequency = measurementFrequency;
  }

  public List<SensorType> getSensors() {

    if (sensors == null) {
      this.sensors = new ArrayList<>();

      if (sensorString != null && !sensorString.isEmpty()) {
        String[] sensorTypeStrings = sensorString.split(",");

        for (String sensorTypeString : sensorTypeStrings) {
          this.sensors.add(SensorType.getSensorTypeById(Integer.parseInt(sensorTypeString)));
        }

      }
    }
    return sensors;
  }

  public void setSensors(List<SensorType> sensors) {
    String sensorString = "";

    if (!sensors.isEmpty()) {
      for (int i = 0; i < sensors.size() - 1; i++) {
        sensorString += sensors.get(i).getIdentifier() + ",";
      }
      sensorString += sensors.get(sensors.size() - 1).getIdentifier();
    }
    this.sensorString = sensorString;
    this.sensors = sensors;
  }

  public Questionnaire getQuestionnaire() {
    return questionnaire;
  }

  public void setQuestionnaire(Questionnaire qustionnaire) {
    this.questionnaire = qustionnaire;
  }

  public void log(final String logTag) {
    Log.d(logTag, "Campaign Specification Retrieved");
    Log.d(logTag, "name: " + this.getName());
    Log.d(logTag, "description: " + this.getDescription());
    Log.d(logTag, "private: " + this.isPrivate());
    Log.d(logTag, "sensors: " + this.getSensors());
    Log.d(logTag, "snapshotLength: " + this.getSnapshotLength());
    Log.d(logTag, "sampleDuration: " + this.getSampleDuration());
    Log.d(logTag, "sampleFrequency: " + this.getSampleFrequency());
    Log.d(logTag, "measurementFrequency: " + this.getMeasurementFrequency());
    Log.d(logTag, "campaignLength: " + this.getCampaignLength());
    Log.d(logTag, "questionnairePlacement: " + this.getQuestionnairePlacement());

    if (this.getQuestionnaire() != null) {
      String questions = "";
      for (Question question : this.getQuestionnaire().getQuestions()) {
        questions += question.getIdentifier() + ": " + question.getQuestion() + ",";
      }
      Log.d(logTag, "questions: " + questions);
    }
  }

  public List<RealmObject> children() {

    List<RealmObject> children = new ArrayList<>();
    children.add(this);
    for (Snapshot snapshot : snapshots) {
      children.addAll(snapshot.children());
    }
    return children;
  }

  public void setCampaignLength(int campaignLength) {
    this.campaignLength = campaignLength;
  }

  public int getCampaignLength() {
    return campaignLength;
  }

  public void setQuestionnairePlacement(QuestionnairePlacement questionnairePlacement) {
    this.questionnairePlacement = questionnairePlacement;
    this.placementId = questionnairePlacement.getIdentifier();
  }

  public QuestionnairePlacement getQuestionnairePlacement() {
    if (this.questionnairePlacement == null) {
      questionnairePlacement = QuestionnairePlacement.getQuestionnairePlacementById(placementId);
    }
    return questionnairePlacement;
  }

  public boolean isSnapshotReady(final Snapshot snapshot) {

    final long currentTime = System.currentTimeMillis();

    return isSnapshotReady(currentTime, snapshot);
  }

  public boolean isSnapshotReady(final long timestamp, final Snapshot snapshot) {

    switch (getQuestionnairePlacement()) {
      case START:
        return timestamp > (snapshot.getTimestamp() + this.getSnapshotLength());
      case END:
        return snapshot.getQuestionnaire() != null || timestamp > (snapshot.getTimestamp() + this.getSnapshotLength() * 2);
      default:
        return false;
    }
  }
}
