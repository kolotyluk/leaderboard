package unit

import net.kolotyluk.leaderboard.scorekeeping._
import net.kolotyluk.scala.extras.Logging
import org.scalatest.{FlatSpec, GivenWhenThen, Matchers}

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

class LeaderboardSpec
  extends FlatSpec
    with GivenWhenThen
    with Matchers
    with Logging {

  behavior of "Leaderboard"

  it must "handle initial conditions correctly" in {

    val joeBlow = "Joe Blow"

    val scoreKeeper = new Leaderboard()
    Given("a new ScoreKeeper")

    scoreKeeper.getCount() should be (0)
    scoreKeeper.getScore(joeBlow) should be (None)
    scoreKeeper.getStanding(joeBlow) should be (None)
    Then("there should be no scores or standings")

    var range = scoreKeeper.getRange(0, 1)
    range.totalCount should be (0)
    range.placings.size should be (0)
    Then("the range should be empty")

    ////////////////////////////////////////////////////////

    scoreKeeper.update(Replace, joeBlow, Score(BigInt(1)))
    Given("a score of 1 is set")

    scoreKeeper.getCount() should be (1)
    Then("there should be 1 score")

    scoreKeeper.getScore(joeBlow) should be (Some(1))
    Then("the score should be 1")

    scoreKeeper.getStanding(joeBlow) should be (Some(Standing(1,1)))
    Then("the standing should be 1 of 1")

    range = scoreKeeper.getRange(0, 1)
    range.totalCount should be (1)
    range.placings.size should be (1)
    val placing = range.placings.head
    placing.member should be (joeBlow)
    placing.score should be (1)
    Then("the should contain the member and their score")

    // This next test is highly paranoid because Scala has better string handling than Java,
    // with respect to string comparison, but better to be paranoid, than have incorrect code.

    val joeBlow2 = new String(joeBlow)
    Given("same member with a different string")

    // https://www.slideshare.net/knoldus/object-equality-inscala (5 of 33)
    assert(joeBlow ne joeBlow2)
    Then("the strings should have different object identifiers")

    scoreKeeper.getStanding("Joe " + "Blow") should be (Some(Standing(1,1)))
    Then("the standing should still be 1 of 1")

    scoreKeeper.update(Increment, joeBlow, Score(BigInt(1)))
    When("the score is incremented")

    scoreKeeper.getScore(joeBlow) should be (Some(2))
    Then("it should be incremented correctly")

    scoreKeeper.getStanding(joeBlow) should be (Some(Standing(1,1)))
    Then("the standing should still be 1 of 1")

    ////////////////////////////////////////////////////////

    scoreKeeper.delete(joeBlow)
    Given("a member is deleted from the leaderboard")

    scoreKeeper.getScore(joeBlow) should be (None)
    scoreKeeper.getStanding(joeBlow) should be (None)
    Then("they should have no scores or standings")

    range = scoreKeeper.getRange(0, 1)
    range.totalCount should be (0)
    range.placings.size should be (0)
    Then("the range should be empty")
  }

  it must "handle 2 members correctly" in {

    val scoreKeeper = new Leaderboard()
    Given("a new ScoreKeeper")

    val joeBlow = "Joe Blow"
    val janeBlow = "Jane Blow"

    scoreKeeper.update(Replace, joeBlow, Score(BigInt(1)))
    scoreKeeper.update(Replace, janeBlow, Score(BigInt(1)))

    When("2 members score")

    var range = scoreKeeper.getRange(0, 1)

    range.totalCount should be (2)
    val placings = range.placings
    placings.size should be (2)
    if (placings.head.member == joeBlow) {
      When(s"one member is $joeBlow")
      placings.tail.head.member should be (janeBlow)
      Then(s"the other member is $janeBlow")
    } else {
      When(s"one member is $janeBlow")
      placings.tail.head.member should be (joeBlow)
      Then(s"the other member is $joeBlow")
    }

    var joeWins = 0
    var janeWins = 0

    val scores = 100

    for (i <- 1 to scores) {
      scoreKeeper.update(Increment, joeBlow, 1)
      scoreKeeper.update(Increment, janeBlow, 1)
      range = scoreKeeper.getRange(0, 1)
      // println(s"range = $range")
      if (range.placings.head.member === joeBlow) joeWins += 1 else janeWins += 1
    }

    Given(s"each member with $scores equal scores")
    When(s"joeWins = $joeWins, janeWins = $janeWins")

    joeWins should be > 30
    janeWins should be > 30
    Then("each should win roughly half the time")

    println(range)

  }

  it must "handle concurrent updates correctly" in {

    val scoreKeeper = new Leaderboard()
    Given(s"a JVM with ${ Runtime.getRuntime().availableProcessors()} available processors")

    val joeBlow = "Joe Blow"
    val janeBlow = "Jane Blow"

    var range = scoreKeeper.getRange(0, 1)

    var joeWins = 0
    var janeWins = 0

    val scores = 100

    val futures = new ArrayBuffer[Future[Unit]]

    When(s"each member is updated $scores times concurrently")

    for (i <- 1 to scores) {
      futures.append(Future {
        scoreKeeper.update(Increment, joeBlow, 1)
        //logger.debug(s"$joeBlow += 1")
      })
      futures.append(Future {
        scoreKeeper.update(Increment, janeBlow, 1)
        //logger.debug(s"$janeBlow += 1")
      })
      //range = scoreKeeper.getRange(0, 1)
      // println(s"range = $range")
      //if (range.placings.head.member === joeBlow) joeWins += 1 else janeWins += 1
    }

    val done = Future.sequence(futures)

//    done.onComplete {
//      case Failure(cause) => println("error")
//      case Success(value) =>
//    }

    val w = Await.result(done, 10 seconds)

    range = scoreKeeper.getRange(0, 1)
    println(s"range = $range")

    val joePlacing = range.placings.head
    val janePlacing = range.placings.head

    range.totalCount should be (2)
    joePlacing.score should be (scores)
    janePlacing.score should be (scores)

    Then(s"each member's score should be $scores")
    And("there should be no concurrency errors")


    println()
  }
}
