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
class GrpcSimulationIT extends Simulation with Configuration {

  val duration = 60 seconds
  val repeat = 10
  val users = 10

  setUp(
    createLeaderboardScenario("SynchronizedLeaderboard").inject(
        atOnceUsers(1))
      .protocols(httpProtocol),
    grpcScenario(grpcChain, repeat).inject(
        nothingFor(4 seconds),
        rampUsers(users) during duration)
      .protocols(grpcProtocol)
  )

}