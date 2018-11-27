package unit

import net.kolotyluk.leaderboard.scorekeeping._
import net.kolotyluk.leaderboard.telemetry.Metrics
import net.kolotyluk.scala.extras.Logging
import org.scalatest.{FlatSpec, GivenWhenThen, Matchers, SequentialNestedSuiteExecution}

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.{Await,Future}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Random, Success}

class LeaderboardSpec extends UnitSpec {

  behavior of "Leaderboard"

  it must "handle initial conditions correctly" in {

    val joeBlow = "Joe Blow"

    val leaderboard = Leaderboard.add match {
      case Failure(cause) => throw cause
      case Success(leaderboard) => leaderboard
    }
    Given("a new ScoreKeeper")

    leaderboard.getCount should be (0)
    leaderboard.getScore(joeBlow) should be (None)
    leaderboard.getStanding(joeBlow) should be (None)
    Then("there should be no scores or standings")

    var range = leaderboard.getRange(0, 1)
    range.totalCount should be (0)
    range.placings.size should be (0)
    Then("the range should be empty")

    ////////////////////////////////////////////////////////

    leaderboard.update(Increment, joeBlow, Score(BigInt(1)))
    Given("a score of 1 is set")

    leaderboard.getCount should be (1)
    Then("there should be 1 score")

    leaderboard.getScore(joeBlow) should be (Some(1))
    Then("the score should be 1")

    leaderboard.getStanding(joeBlow) should be (Some(Standing(1,1)))
    Then("the standing should be 1 of 1")

    range = leaderboard.getRange(0, 1)
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

    leaderboard.getStanding("Joe " + "Blow") should be (Some(Standing(1,1)))
    Then("the standing should still be 1 of 1")

    leaderboard.update(Increment, joeBlow, Score(BigInt(1)))
    When("the score is incremented")

    leaderboard.getScore(joeBlow) should be (Some(2))
    Then("it should be incremented correctly")

    leaderboard.getStanding(joeBlow) should be (Some(Standing(1,1)))
    Then("the standing should still be 1 of 1")

    ////////////////////////////////////////////////////////

    leaderboard.delete(joeBlow)
    Given("a member is deleted from the leaderboard")

    leaderboard.getScore(joeBlow) should be (None)
    leaderboard.getStanding(joeBlow) should be (None)
    Then("they should have no scores or standings")

    range = leaderboard.getRange(0, 1)
    range.totalCount should be (0)
    range.placings.size should be (0)
    Then("the range should be empty")
  }

  it must "handle 2 members correctly" in {

    val leaderboard = Leaderboard.add match {
      case Failure(cause) => throw cause
      case Success(leaderboard) => leaderboard
    }
    Given("a new ScoreKeeper")

    val joeBlow = "Joe Blow"
    val janeBlow = "Jane Blow"

    leaderboard.update(Replace, joeBlow, Score(BigInt(1)))
    leaderboard.update(Replace, janeBlow, Score(BigInt(1)))

    When("2 members score")

    var range = leaderboard.getRange(0, 1)

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
      leaderboard.update(Increment, joeBlow, 1)
      leaderboard.update(Increment, janeBlow, 1)
      range = leaderboard.getRange(0, 1)
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

    val leaderboard = Leaderboard.add match {
      case Failure(cause) => throw cause
      case Success(leaderboard) => leaderboard
    }
    Given(s"a JVM with ${ Runtime.getRuntime().availableProcessors()} available processors")

    val joeBlow = "Joe Blow"
    val janeBlow = "Jane Blow"

    var range = leaderboard.getRange(0, 1)

    var joeWins = 0
    var janeWins = 0

    val scores = 100

    val futures = new ArrayBuffer[Future[Unit]]

    When(s"each member is updated $scores times concurrently")

    for (i <- 1 to scores) {
      futures.append(Future {
        leaderboard.update(Increment, joeBlow, 1)
        //logger.debug(s"$joeBlow += 1")
      })
      futures.append(Future {
        leaderboard.update(Increment, janeBlow, 1)
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

    val result = Await.result(done, 10 seconds)

    range = leaderboard.getRange(0, 1)
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

  it must "handle high intensity concurrent updates correctly" in {

    Metrics.resetLargestSpinCount;  info(s"LargestSpinCount = ${Metrics.getLargestSpinCount}")
    Metrics.resetLargetsSpinTime;   info(s"LargestSpinTime  = ${Metrics.getLargestSpinTime}")
    Metrics.resetTotalSpinCount;    info(s"TotalSpinCount  = ${Metrics.getTotalSpinCount}")
    Metrics.resetTotalSpinTime;     info(s"TotalSpinTime  = ${Metrics.getTotalSpinTime}")

    val leaderboard = Leaderboard.add match {
      case Failure(cause) => throw cause
      case Success(leaderboard) => leaderboard
    }

    val joeBlow = "Joe Blow"

    val iterations = 1000
    val availableProcessors = Runtime.getRuntime().availableProcessors()
    Given(s"a JVM with $availableProcessors available processors")

    val futures = new ArrayBuffer[Future[Unit]]

    val startTime = System.nanoTime()

    for (processor <- 1 to availableProcessors) {
      futures.append(Future{
        for (update <- 1 to iterations) {
          leaderboard.update(Increment, joeBlow, 1)
        }
      })
    }

    val done = Future.sequence(futures)

    val result = Await.result(done, 100 seconds)
    val elapsedTime = System.nanoTime - startTime
    val transactionsPerSecond: Double = ((iterations * availableProcessors).toDouble  / elapsedTime) * 1000000000

    val spinFraction = Metrics.getTotalSpinTime.toDouble / (elapsedTime.toDouble * availableProcessors)

    When(s"iterations = $iterations, elapsedTime = $elapsedTime nanoseconds, transactionsPerSecond = $transactionsPerSecond")
    And(s"largestSpinCount = ${Metrics.getLargestSpinCount}, totalSpinCount = ${Metrics.getTotalSpinCount},  largetstSpinMember = ${Metrics.largestSpinMember}, msximumSpinCount = ${Metrics.msximumSpinCount}")
    And(s"largestSpinTime = ${Metrics.getLargestSpinTime} nanoseconds, totalSpinTime = ${Metrics.getTotalSpinTime}, spinFraction = ${spinFraction}")

    val expectedScore = availableProcessors * iterations
    leaderboard.getScore(joeBlow).get should be (expectedScore)
    Then(s"$joeBlow's score should be $expectedScore")

  }

  it must "handle high a large number of members" in {

    Metrics.resetLargestSpinCount;  info(s"LargestSpinCount = ${Metrics.getLargestSpinCount}")
    Metrics.resetLargetsSpinTime;   info(s"LargestSpinTime  = ${Metrics.getLargestSpinTime}")
    Metrics.resetTotalSpinCount;    info(s"TotalSpinCount  = ${Metrics.getTotalSpinCount}")
    Metrics.resetTotalSpinTime;     info(s"TotalSpinTime  = ${Metrics.getTotalSpinTime}")

    val leaderboard = Leaderboard.add match {
      case Failure(cause) => throw cause
      case Success(leaderboard) => leaderboard
    }

    val random = new Random

//    for (m <- 1 to 1000000) {
//      //val member = s"member ${Math.abs(random.nextInt(1000000))}"
//      val member = s"member ${m}"
//      leaderboard.update(Increment, member, m)
//    }

    val iterations = 1000
    val availableProcessors = Runtime.getRuntime().availableProcessors()

    val futures = new ArrayBuffer[Future[Unit]]

    val startTime = System.nanoTime()

    for (processor <- 1 to availableProcessors) {
      futures.append(Future{
        for (update <- 1 to iterations) {
          val member = s"member ${Math.abs(random.nextInt(iterations * availableProcessors))}"
          // info(s"updating $member")
          leaderboard.update(Increment, member, 1)
        }
      })
    }

    val done = Future.sequence(futures)

    val result = Await.result(done, 100 seconds)
    val elapsedTime = System.nanoTime - startTime
    val transactionsPerSecond: Double = ((iterations * availableProcessors).toDouble  / elapsedTime) * 1000000000

    val spinFraction = Metrics.getTotalSpinTime.toDouble / (elapsedTime.toDouble * availableProcessors)

    When(s"iterations = $iterations, elapsedTime = $elapsedTime nanoseconds, transactionsPerSecond = $transactionsPerSecond")
    And(s"largestSpinCount = ${Metrics.getLargestSpinCount}, totalSpinCount = ${Metrics.getTotalSpinCount},  largetstSpinMember = ${Metrics.largestSpinMember}, msximumSpinCount = ${Metrics.msximumSpinCount}")
    And(s"largestSpinTime = ${Metrics.getLargestSpinTime} nanoseconds, totalSpinTime = ${Metrics.getTotalSpinTime}, spinFraction = ${spinFraction}")

    val range = leaderboard.getRange(0, leaderboard.getCount)

    range.placings.foreach(placing => println(s" ${placing.member} ${placing.place} ${placing.score}"))

  }
}
