package net.kolotyluk.leaderboard.scorekeeping

import java.util.concurrent.ConcurrentLinkedQueue

import net.kolotyluk.leaderboard.telemetry.Metrics
import net.kolotyluk.scala.extras.Logging
import unit.UnitSpec

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.language.postfixOps
import scala.util.Random

/** =Leaderboard Unit Test Behaviors=
  * Standard behaviors of the Leaderboard interface for all implementations
  * <p>
  * Example use:
  * {{{
  * it must behave like handleInitialConditions(leaderboard)
  * it must behave like handleTwoMembers(leaderboard)
  * it must behave like handleHandleConcurrentUpdates(leaderboard)
  * it must behave like handleHandleHighIntensityConcurrentUpdates(leaderboard)
  * it must behave like handleHandleLargeNumberOfMembers(leaderboard)
  * }}}
  *
  * @param this test specification type
  */
trait LeaderboardBehaviors extends Logging { this: UnitSpec =>

  sealed trait API
  case class Async() extends API
  case class Sync() extends API

  private var api: Any = null

  private def apiType(leaderboard: Leaderboard) = {
    if (api == null) {
      api = leaderboard.isInstanceOf[LeaderboardAsync] match {
        case false =>
          Sync
        case true =>
          Async
      }
    }
    api
  }

  private def finalResult[T](value: Any): T =
    if (value.isInstanceOf[Future[Any]])
      Await.result(value.asInstanceOf[Future[T]], 10 seconds)
    else
      value.asInstanceOf[T]

  // Force logging noise to the beginning of the test runs
  logger.info("logging")

  /** =First Behavior to Verify=
    * Always test this first
    *
    * @param leaderboard
    */
  def handleInitialConditionsAsync(leaderboard: => Leaderboard) {

    apiType(leaderboard) match {
      case Async => logger.info("using LeaderboardAsync")
      case Sync => logger.info("using LeaderboardSync")
    }

    it must "handle initial conditions correctly" in {

      val joeBlow = "Joe Blow"

      Given("a new ScoreKeeper")
      finalResult[Int](leaderboard.getCount) should be (0)
      finalResult[Option[Score]](leaderboard.getScore(joeBlow)) should be (None)
      finalResult[Option[Standing]](leaderboard.getStanding(joeBlow)) should be (None)
      Then("there should be no scores or standings")


      //      val count = apiType(leaderboard) match {
//        case Async => Await.result(leaderboard.getCount.asInstanceOf[Future[Int]], 10 seconds)
//        case Sync => leaderboard.getCount.asInstanceOf[Int]
//      }

//      val result = leaderboard.getCount
//
//      if (result.isInstanceOf[Future[Int]]) {
//        println("***********************************************************************")
//      }

//      Await.result(leaderboard.getCount, 10 seconds) should be (0)
//      Await.result(leaderboard.getScore(joeBlow), 10 seconds) should be (None)
//      Await.result(leaderboard.getStanding(joeBlow), 10 seconds) should be (None)

      var range = finalResult[Range](leaderboard.getRange(0, 1))
      range.totalCount should be (0)
      range.placings.size should be (0)
      Then("the range should be empty")

      ////////////////////////////////////////////////////////

      Given("the score is incremented by 1")
      finalResult[Score](leaderboard.update(Increment, joeBlow, Score(BigInt(1)))).value should be (1)
      Then("the score should be 1")
      finalResult[Int](leaderboard.getCount) should be (1)
      And("there should be only 1 score")
      finalResult[Option[Score]](leaderboard.getScore(joeBlow)) should be (Some(1))
      And("the score should be 1")
      finalResult[Option[Standing]](leaderboard.getStanding(joeBlow)) should be (Some(Standing(1,1)))
      And("the standing should be 1 of 1")

      range = finalResult[Range](leaderboard.getRange(0, 1))
      range.totalCount should be (1)
      range.placings.size should be (1)

      val placing = range.placings.head
      placing.member should be (joeBlow)
      placing.score should be (1)
      And("they should contain the member and their score")

      // This next test is highly paranoid because Scala has better string handling than Java,
      // with respect to string comparison, but better to be paranoid, than have incorrect code.

      val joeBlow2 = new String(joeBlow)
      Given("same member with a different string")

      // https://www.slideshare.net/knoldus/object-equality-inscala (5 of 33)
      assert(joeBlow ne joeBlow2)
      Then("the strings should have different object identifiers")

      finalResult[Option[Standing]](leaderboard.getStanding("Joe " + "Blow")) should be (Some(Standing(1,1)))
      And("the standing should still be 1 of 1")

      finalResult[Score](leaderboard.update(Increment, joeBlow, Score(BigInt(1)))).value should be (2)
      When("the score is incremented")

      finalResult[Option[BigInt]](leaderboard.getScore(joeBlow)) should be (Some(2))
      Then("it should be incremented correctly")

      finalResult[Option[Standing]](leaderboard.getStanding(joeBlow)) should be (Some(Standing(1,1)))
      And("the standing should still be 1 of 1")

      ////////////////////////////////////////////////////////

      finalResult[Boolean](leaderboard.delete(joeBlow)) should be (true)
      Given("a member is deleted from the leaderboard")

      finalResult[Option[BigInt]](leaderboard.getScore(joeBlow)) should be (None)
      finalResult[Option[Standing]](leaderboard.getStanding(joeBlow)) should be (None)
      Then("they should have no scores or standings")

      range = finalResult[Range](leaderboard.getRange(0, 1))
      range.totalCount should be (0)
      range.placings.size should be (0)
      And("the range should be empty")

    }
  }

  def handleTwoMembersAsync(leaderboard: => Leaderboard): Unit = {

    it must "handle 2 members correctly" in {

      val joeBlow = "Joe Blow"
      val janeBlow = "Jane Blow"

      Given(s"two members: $joeBlow and $janeBlow")
      When("2 members score")

      finalResult[Score](leaderboard.update(Replace, joeBlow,  Score(BigInt(1)))).value should be (1)
      finalResult[Score](leaderboard.update(Replace, janeBlow, Score(BigInt(1)))).value should be (1)

      var range = finalResult[Range](leaderboard.getRange(0, 1))

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

      // TODO can this be done more efficiently?
      for (i <- 1 to scores) {
        finalResult[Score](leaderboard.update(Replace, joeBlow,  1))
        finalResult[Score](leaderboard.update(Replace, janeBlow, 1))
        range = finalResult[Range](leaderboard.getRange(0, 1))
        // println(s"range = $range")
        if (range.placings.head.member === joeBlow) joeWins += 1 else janeWins += 1
      }

      Given(s"each member with $scores equal scores")
      When(s"joeWins = $joeWins, janeWins = $janeWins")

      joeWins should be > 30
      janeWins should be > 30
      Then("each should win roughly half the time")

      logger.debug(s"handleTwoMembers: range = $range")
    }
  }

  def handleConcurrentUpdatesAsync(leaderboard: => Leaderboard): Unit = {
    it must "handle concurrent updates correctly" in {

      Given(s"a JVM with ${ Runtime.getRuntime().availableProcessors()} available processors")

      val joeBlow = "Joe Blow"
      val janeBlow = "Jane Blow"

      // reset results of prior testing
      finalResult[Score](leaderboard.update(Replace, joeBlow,  0))
      finalResult[Score](leaderboard.update(Replace, janeBlow, 0))

      var range = finalResult[Range](leaderboard.getRange(0, 1))

      var joeWins = 0
      var janeWins = 0

      val scores = 100

      val futures = new ArrayBuffer[Future[Any]]
      var asyncFutures: Option[ConcurrentLinkedQueue[Future[Score]]] = None

      When(s"each member is updated $scores times concurrently")

      apiType(leaderboard) match {
        case Async =>
          val concurrentFutures = new ConcurrentLinkedQueue[Future[Score]]
          asyncFutures = Some(concurrentFutures)
          for (i <- 1 to scores) {
            futures.append(Future{concurrentFutures.add(leaderboard.update(Increment, joeBlow,  1).asInstanceOf[Future[Score]])})
            futures.append(Future{concurrentFutures.add(leaderboard.update(Increment, janeBlow, 1).asInstanceOf[Future[Score]])})
          }
        case Sync =>
          for (i <- 1 to scores) {
            futures.append(Future {
              finalResult[Score](leaderboard.update(Increment, joeBlow,  1))
              // if (i < 5) logger.debug(s"handleConcurrentUpdates: $joeBlow score = $score")
            })
            futures.append(Future {
              finalResult[Score](leaderboard.update(Increment, janeBlow, 1))
              //logger.debug(s"$janeBlow += 1")
            })
            //range = scoreKeeper.getRange(0, 1)
            // println(s"range = $range")
            //if (range.placings.head.member === joeBlow) joeWins += 1 else janeWins += 1
          }
      }

      val done = Future.sequence(futures)
      val result = Await.result(done, 10 seconds)

      When(s"futures.size = ${result.size}")
      Then(s"futures.size = ${scores * 2}")
      result.size should be (scores * 2)

      asyncFutures.foreach{concurrentFutures =>
        val done = Future.sequence(concurrentFutures.toArray().toList.asInstanceOf[List[Future[Score]]])
        val result = Await.result(done, 10 seconds)
        When(s"concurrentFutures.size = ${result.size}")
        Then(s"concurrentFutures.size = ${scores * 2}")
        result.size should be (scores * 2)
      }

      range =finalResult[Range](leaderboard.getRange(0, 1))
      logger.debug(s"handleHandleConcurrentUpdates: range = $range")

      range.totalCount should be (2)
      range.placings.foreach {  placing =>
        Given(s"member = ${placing.member} score = ${placing.score} place = ${placing.place}")
        placing.score should be (scores)
      }

      //joePlacing.score should be (scores)
      //janePlacing.score should be (scores)

      Then(s"each member's score should be $scores")
      And("there should be no concurrency errors")
    }
  }

  def handleHandleHighIntensityConcurrentUpdatesAsync(leaderboard: => Leaderboard): Unit = {
    it must "handle high intensity concurrent updates correctly" in {

      Metrics.resetLargestSpinCount;  info(s"LargestSpinCount = ${Metrics.getLargestSpinCount}")
      Metrics.resetLargetsSpinTime;   info(s"LargestSpinTime  = ${Metrics.getLargestSpinTime}")
      Metrics.resetTotalSpinCount;    info(s"TotalSpinCount  = ${Metrics.getTotalSpinCount}")
      Metrics.resetTotalSpinTime;     info(s"TotalSpinTime  = ${Metrics.getTotalSpinTime}")

      val joeBlow = "Joe Blow"
      // reset results of prior testing
      leaderboard.update(Replace, joeBlow, 0)

      val iterations = 100
      val availableProcessors = Runtime.getRuntime().availableProcessors()
      Given(s"a JVM with $availableProcessors available processors")

      val futures = new ArrayBuffer[Future[Unit]]

      val startTime = System.nanoTime()

      for (processor <- 1 to availableProcessors) {
        futures.append(Future{
          for (update <- 1 to iterations) {
            val score = leaderboard.update(Increment, joeBlow, 1)
            //logger.debug(s"Joe's score = $score")
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
      Then(s"$joeBlow's score should be $expectedScore")
      finalResult[Option[Score]](leaderboard.getScore(joeBlow)).get should be (expectedScore)
    }
  }

  def handleHandleLargeNumberOfMembersAsync(leaderboard: => Leaderboard): Unit = {
    it must "handle a large number of members" in {

      Metrics.resetLargestSpinCount;  info(s"LargestSpinCount = ${Metrics.getLargestSpinCount}")
      Metrics.resetLargetsSpinTime;   info(s"LargestSpinTime  = ${Metrics.getLargestSpinTime}")
      Metrics.resetTotalSpinCount;    info(s"TotalSpinCount  = ${Metrics.getTotalSpinCount}")
      Metrics.resetTotalSpinTime;     info(s"TotalSpinTime  = ${Metrics.getTotalSpinTime}")

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

      val range = leaderboard.getRange(0, finalResult[Int](leaderboard.getCount))

      // range.placings.foreach(placing => println(s" ${placing.member} ${placing.place} ${placing.score}"))
    }
  }

  def handleInitialConditions(leaderboard: => LeaderboardSync) {

    it must "handle initial conditions correctly" in {

      val joeBlow = "Joe Blow"

      Given("a new leaderboard")

      val result = leaderboard.getCount

      Then("there should be no scores or standings")

      leaderboard.getCount should be (0)
      leaderboard.getScore(joeBlow) should be (None)
      leaderboard.getStanding(joeBlow) should be (None)

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
      Then("they should contain the member and their score")

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
  }

  def handleTwoMembers(leaderboard: => LeaderboardSync): Unit = {

    it must "handle 2 members correctly" in {

      Given("a new ScoreKeeper")

      val joeBlow = "Joe Blow"
      val janeBlow = "Jane Blow"

      leaderboard.update(Replace, joeBlow, Score(BigInt(1)))
      leaderboard.update(Replace, janeBlow, Score(BigInt(1)))

      When("2 members score")

      var range: Range = leaderboard.getRange(0, 1)

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

      logger.debug(s"handleTwoMembers: range = $range")
    }
  }

  def handleConcurrentUpdates(leaderboard: => LeaderboardSync): Unit = {
    it must "handle concurrent updates correctly" in {

      Given(s"a JVM with ${ Runtime.getRuntime().availableProcessors()} available processors")

      val joeBlow = "Joe Blow"
      val janeBlow = "Jane Blow"

      // reset results of prior testing
      leaderboard.update(Replace, joeBlow, 0)
      leaderboard.update(Replace, janeBlow, 0)

      var range = leaderboard.getRange(0, 1)

      var joeWins = 0
      var janeWins = 0

      val scores = 100

      val futures = new ArrayBuffer[Future[Unit]]

      When(s"each member is updated $scores times concurrently")

      for (i <- 1 to scores) {
        futures.append(Future {
          val score = leaderboard.update(Increment, joeBlow, 1)
          if (i < 5) logger.debug(s"handleConcurrentUpdates: $joeBlow score = $score")
        })
        futures.append(Future {
          val score = leaderboard.update(Increment, janeBlow, 1)
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

      result.size should be (scores * 2)

      range = leaderboard.getRange(0, 1)
      logger.debug(s"handleHandleConcurrentUpdates: range = $range")

      range.totalCount should be (2)
      range.placings.foreach {  placing =>
        Given(s"member = ${placing.member} score = ${placing.score} place = ${placing.place}")
        placing.score should be (scores)
      }

      //joePlacing.score should be (scores)
      //janePlacing.score should be (scores)

      Then(s"each member's score should be $scores")
      And("there should be no concurrency errors")
    }
  }

  def handleHandleHighIntensityConcurrentUpdates(leaderboard: => LeaderboardSync): Unit = {
    it must "handle high intensity concurrent updates correctly" in {

      Metrics.resetLargestSpinCount;  info(s"LargestSpinCount = ${Metrics.getLargestSpinCount}")
      Metrics.resetLargetsSpinTime;   info(s"LargestSpinTime  = ${Metrics.getLargestSpinTime}")
      Metrics.resetTotalSpinCount;    info(s"TotalSpinCount  = ${Metrics.getTotalSpinCount}")
      Metrics.resetTotalSpinTime;     info(s"TotalSpinTime  = ${Metrics.getTotalSpinTime}")

      val joeBlow = "Joe Blow"
      // reset results of prior testing
      leaderboard.update(Replace, joeBlow, 0)

      val iterations = 100
      val availableProcessors = Runtime.getRuntime().availableProcessors()
      Given(s"a JVM with $availableProcessors available processors")

      val futures = new ArrayBuffer[Future[Unit]]

      val startTime = System.nanoTime()

      for (processor <- 1 to availableProcessors) {
        futures.append(Future{
          for (update <- 1 to iterations) {
            val score = leaderboard.update(Increment, joeBlow, 1)
            //logger.debug(s"Joe's score = $score")
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
      Then(s"$joeBlow's score is should be $expectedScore")
      leaderboard.getScore(joeBlow).get should be (expectedScore)
    }
  }

  def handleHandleLargeNumberOfMembers(leaderboard: => LeaderboardSync): Unit = {
    it must "handle a large number of members" in {

      Metrics.resetLargestSpinCount;  info(s"LargestSpinCount = ${Metrics.getLargestSpinCount}")
      Metrics.resetLargetsSpinTime;   info(s"LargestSpinTime  = ${Metrics.getLargestSpinTime}")
      Metrics.resetTotalSpinCount;    info(s"TotalSpinCount  = ${Metrics.getTotalSpinCount}")
      Metrics.resetTotalSpinTime;     info(s"TotalSpinTime  = ${Metrics.getTotalSpinTime}")

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

      // range.placings.foreach(placing => println(s" ${placing.member} ${placing.place} ${placing.score}"))

    }

  }

    //  def nonEmptyStack(newStack: => Stack[Int], lastItemAdded: Int) {
//
//    it should "be non-empty" in {
//      assert(!newStack.empty)
//    }
//
//    it should "return the top item on peek" in {
//      assert(newStack.peek === lastItemAdded)
//    }
//
//    it should "not remove the top item on peek" in {
//      val stack = newStack
//      val size = stack.size
//      assert(stack.peek === lastItemAdded)
//      assert(stack.size === size)
//    }
//
//    it should "remove the top item on pop" in {
//      val stack = newStack
//      val size = stack.size
//      assert(stack.pop === lastItemAdded)
//      assert(stack.size === size - 1)
//    }
//  }
//
//  def nonFullStack(newStack: => Stack[Int]) {
//
//    it should "not be full" in {
//      assert(!newStack.full)
//    }
//
//    it should "add to the top on push" in {
//      val stack = newStack
//      val size = stack.size
//      stack.push(7)
//      assert(stack.size === size + 1)
//      assert(stack.peek === 7)
//    }
//  }
}