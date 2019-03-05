package it.gatling

import io.gatling.core.Predef._
import net.kolotyluk.scala.extras.Configuration

import scala.concurrent.duration._
import scala.language.postfixOps

/** =Gatling Scoring Simulation=
  *
  * Simulate a scoring simulation where leaderboard members can directly add their scores to a leaderboard.
  * <p>
  *
  *
  * ==Test Strategy==
  *
  * ===Test Levels===
  *
  * ===Roles and Responsibilities===
  *
  * ===Environment Requirements===
  *
  * === Testing Tools===
  *
  * ===Risks and Mitigation===
  *
  * ===Test Schedule===
  *
  * ==Test Plan==
  *
  * ===Test Coverage===
  *
  * ===Test Methods===
  *
  * ===Test Responsibilities===
  *
  * ==Test Suite==
  *
  *   1. Create Leaderboard(s)
  *   1. Update Leaderboard(s)
  *
  * <p>
  * usage: `mvn gatling:test`, or `mvn verify`
  * {{{
  * }}}
  * @see [[https://en.wikipedia.org/wiki/Test_strategy Test Strategy]]
  * @see [[https://en.wikipedia.org/wiki/Test_plan Test Plan]]
  * @see [[https://en.wikipedia.org/wiki/Test_suite Test Suite]]
  * @see [[https://en.wikipedia.org/wiki/Test_case Test Case]]
  */
class BulkScoreSimulationIT extends Simulation with Configuration {

  val bulkSize = 2
  val scenario = bulkUpdateLeaderboardChain(bulkSize)
  val repeat = 150
  val users = 100000 / 2
  val span = 600 seconds

  setUp(
    createLeaderboardScenario.inject(
        atOnceUsers(1))
      .protocols(httpProtocol),
    updateScenario(scenario, repeat).inject(
        nothingFor(4 seconds),
        rampUsers(users) during (span))
      .protocols(httpUpdateProtocol)
  )

  // updateSetUp(100)

}