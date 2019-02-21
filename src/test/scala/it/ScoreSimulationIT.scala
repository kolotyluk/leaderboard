package it.gatling

import io.gatling.core.Predef._
import net.kolotyluk.scala.extras.Configuration
import scala.concurrent.duration._

import scala.language.postfixOps


/** =Basic Gatling Simulation=
  * Ping the leaderboard service
  * <p>
  * usage: `mvn gatling:test`
  * {{{
  * }}}
  */
class ScoreSimulationIT extends Simulation with Configuration {

  val testHost = config.getString("gatling.test.host")

  val requestCount = 100000
  val userCount = 1000

  setUp(
    createLeaderboardScenario.inject(
        atOnceUsers(1))
      .protocols(httpProtocol),
    updateLeaderboardScenario.inject(
        nothingFor(4 seconds),
        rampUsers(10) during (20 seconds))
      .protocols(httpProtocol)
  )
}