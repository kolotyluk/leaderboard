package it.gatling

import io.gatling.core.Predef._
import net.kolotyluk.scala.extras.Configuration

import scala.language.postfixOps

import scala.concurrent.duration._

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
class ScoreSimulationIT extends Simulation with Configuration {

  setUp(
    createLeaderboardScenario("LeaderboardSync").inject(
        atOnceUsers(1))
      .protocols(httpProtocol),
    updateLeaderboardScenario.inject(
        nothingFor(4 seconds),
        rampUsers(100000) during (600 seconds))
      .protocols(httpProtocol)
      //.protocols(httpProtocol.shareConnections)
  )

}