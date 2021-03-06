package dk.aau.sw808f16.datacollection.questionaire.models;

import android.os.Parcel;
import android.os.Parcelable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

import dk.aau.sw808f16.datacollection.snapshot.JsonObjectAble;
import io.realm.RealmList;
import io.realm.RealmObject;

public class Questionnaire extends RealmObject implements Parcelable, JsonObjectAble {

  private RealmList<Question> questions;
  private int currentQuestionIndex = -1;

  public static final Parcelable.Creator<Questionnaire> CREATOR = new Creator<Questionnaire>() {
    @Override
    public Questionnaire createFromParcel(final Parcel source) {
      return new Questionnaire(source);
    }

    @Override
    public Questionnaire[] newArray(int size) {
      return new Questionnaire[size];
    }
  };

  public Questionnaire() {
    this.questions = new RealmList<>();
  }

  public Questionnaire(final Questionnaire questionnaire) {
    this.questions = new RealmList<>();
    for (Question question : questionnaire.getQuestions()) {
      this.questions.add(new Question(question));
    }
  }

  public Questionnaire(List<Question> questions) {
    this();
    this.questions.addAll(questions);
  }

  private Questionnaire(Parcel parcel) {
    this(); // Call constructor to initialize fields
    parcel.readList(questions, Question.class.getClassLoader());
    this.currentQuestionIndex = parcel.readInt();
  }

  public Questionnaire(JSONObject jsonObject) throws JSONException {
    this(); // Call constructor to initialize fields

    final JSONArray jsonQuestions = jsonObject.getJSONArray("questions");

    for (int counter = 0; counter < jsonQuestions.length(); counter++) {
      questions.add(new Question(jsonQuestions.getJSONObject(counter)));
    }

  }

  public List<Question> getQuestions() {
    return questions;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || !(obj instanceof Questionnaire)) {
      return false;
    }

    Questionnaire instance = (Questionnaire) obj;

    return this.getQuestions().equals(instance.getQuestions());
  }

  // Gets the next question
  public Question getNextQuestion() {
    this.currentQuestionIndex++;
    return questions.get(currentQuestionIndex);
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeList(questions);
    dest.writeInt(currentQuestionIndex);
  }

  @Override
  public JSONObject toJsonObject() throws JSONException {
    JSONObject jsonObject = new JSONObject();
    JSONArray questionsArray = new JSONArray();
    for (Question question : questions) {
      questionsArray.put(question.toJsonObject());
    }

    jsonObject.put("questions", questionsArray);
    return jsonObject;
  }
}
