package unit

import net.kolotyluk.leaderboard.scorekeeping._
import org.scalatest.{FlatSpec, GivenWhenThen, Matchers}

class LeaderboardSpec
  extends FlatSpec
    with GivenWhenThen
    with Matchers {

  behavior of "Leaderboard"

  it must "handle initial conditions correctly" in {

    val joeBlow = "Joe Blow"

    val scoreKeeper = new ScoreKeeper()
    Given("a new ScoreKeeper")

    scoreKeeper.getCount() should be (0)
    scoreKeeper.getScore(joeBlow) should be (None)
    scoreKeeper.getStanding(joeBlow) should be (None)
    Then("There should be no scores or standings")

    scoreKeeper.update(Replace, joeBlow, Score(BigInt(1)))
    Given("a score of 1 is set")

    scoreKeeper.getCount() should be (1)
    Then("There should be 1 score")

    scoreKeeper.getScore(joeBlow) should be (Some(1))
    Then("The score should be 1")

    scoreKeeper.getStanding(joeBlow) should be (Some(Standing(1,1)))
    Then("The standing should be 1 of 1")

    // This next test is highly paranoid because Scala has better string handling than Java,
    // with respect to string comparison, but better to be paranoid, than have incorrect code.

    val joeBlow2 = new String(joeBlow)
    Given("Same member with a different string")

    // https://www.slideshare.net/knoldus/object-equality-inscala (5 of 33)
    assert(joeBlow ne joeBlow2)
    Then("The strings should have different object identifiers")

    scoreKeeper.getStanding("Joe " + "Blow") should be (Some(Standing(1,1)))
    Then("The standing should still be 1 of 1")

    scoreKeeper.update(Increment, joeBlow, Score(BigInt(1)))
    When("The score is incremented")

    scoreKeeper.getScore(joeBlow) should be (Some(2))
    Then("It should be incremented correctly")

    scoreKeeper.getStanding(joeBlow) should be (Some(Standing(1,1)))
    Then("The standing should still be 1 of 1")

    scoreKeeper.delete(joeBlow)
    Given("a member is deleted from the leaderboard")

    scoreKeeper.getScore(joeBlow) should be (None)
    scoreKeeper.getStanding(joeBlow) should be (None)
    Then("They should have no scores or standings")

  }

}
