package it

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

/** =Basic Gatling Simulation=
  * Ping the leaderboard service
  * <p>
  * usage: `mvn gatling:test`
  */
class GatlingPingSimulationIT extends Simulation {

  val httpProtocol = http
    //.baseUrl("http://localhost:8080") // Here is the root for all relative URLs
    .baseUrl("http://192.168.0.19:8080") // Here is the root for all relative URLs
    .acceptHeader("text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8") // Here are the common headers
    .acceptEncodingHeader("gzip, deflate")
    .acceptLanguageHeader("en-US,en;q=0.5")
    .userAgentHeader("Mozilla/5.0 (Macintosh; Intel Mac OS X 10.8; rv:16.0) Gecko/20100101 Firefox/16.0")

  val scn = scenario("Ping") // A scenario is a chain of requests and pauses
    .exec(http("request_1")
    .get("/ping"))
    .pause(7) // Note that Gatling has recorder real time pauses

  setUp(scn.inject(atOnceUsers(1)).protocols(httpProtocol))
}