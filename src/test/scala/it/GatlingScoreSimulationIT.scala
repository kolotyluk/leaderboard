package it

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import net.kolotyluk.scala.extras.Configuration

/** =Basic Gatling Simulation=
  * Ping the leaderboard service
  * <p>
  * usage: `mvn gatling:test`
  * {{{
  * }}}
  */
class GatlingScoreSimulationIT extends Simulation with Configuration {

  val testHost = config.getString("gatling.test.host")

  val requestCount = 100000
  val userCount = 1000

  val httpProtocol = http
    //.baseUrl("http://localhost:8080") // Here is the root for all relative URLs
    .baseUrl(s"http://$testHost") // Here is the root for all relative URLs
    .acceptHeader("text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8") // Here are the common headers
    .acceptEncodingHeader("gzip, deflate")
    .acceptLanguageHeader("en-US,en;q=0.5")
    .userAgentHeader("Mozilla/5.0 (Macintosh; Intel Mac OS X 10.8; rv:16.0) Gecko/20100101 Firefox/16.0")

  object Create {

    val create = exec(http("Create leaderboard")
      .post("/leaderboard")
      .check(status.is(201), jsonPath("$..id").ofType[String].saveAs("leaderboardId")))
  }

  val createScenario = scenario("Create scenario").exec(Create.create)

  setUp(createScenario.inject(atOnceUsers(1)).protocols(httpProtocol))

//  val simpleUser = scenario("Ping") // A scenario is a chain of requests and pauses
//      .repeat(requestCount / userCount) {
//          exec(http("ping request").get("/ping"))
//        }
//
//  setUp(simpleUser.inject(atOnceUsers(userCount)).protocols(httpProtocol))
}