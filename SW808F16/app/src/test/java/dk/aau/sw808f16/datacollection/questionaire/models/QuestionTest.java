package dk.aau.sw808f16.datacollection.questionaire.models;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.junit.Test;

public class QuestionTest extends TestCase {


  private final String question = "How are you?";

  @Test
  public void testNormalConstructor() {


    Question question = new Question(this.question);

    Assert.assertEquals("Questions are not the same", this.question, question.getQuestion());
  }

  @Test
  public void testNormalConstructorEmptyInput() {
    String questionString = "";

    try {
      Question question = new Question(questionString);
      fail("The constructor did not throw an exception");
    } catch (Exception exception) {
      assertTrue(exception instanceof IllegalArgumentException);
      assertEquals(exception.getMessage(), "Question cannot be empty");
    }
  }

  @Test
  public void testNormalConstructorNullInput() {
    String questionString = null;

    try {
      Question question = new Question(questionString);
      fail("The constructor did not throw an exception");
    } catch (Exception exception) {
      assertTrue(exception instanceof NullPointerException);
      assertEquals(exception.getMessage(), "Question cannot be null");
    }
  }

  @Test
  public void testAnswerQuestion() {
    Question question = new Question(this.question);
    question.setAnswer(true);
  }

  @Test
  public void testGetAnswer() {

    Question question = new Question(this.question);
    question.setAnswer(false);

    assertEquals("Answer not as expected", false, question.getAnswer());
  }

  @Test
  public void testEquals() {
    Question question1 = new Question(this.question);
    Question question2 = new Question(this.question);

    assertEquals(question1, question2);
  }

  @Test
  public void testNotEquals() {
    Question question1 = new Question(this.question);
    Question question2 = new Question(this.question + " How are you feeling?");

    assertFalse(question1.equals(question2));
  }

  @Test
  public void testReferenceEquals() {
    Question question1 = new Question(this.question);

    assertEquals(question1, question1);
  }
}