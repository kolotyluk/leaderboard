package it.gatling

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import net.kolotyluk.scala.extras.Configuration

/** =Basic Gatling Simulation=
  * Ping the leaderboard service
  * <p>
  * usage: `mvn gatling:test`
  * {{{
  * ================================================================================
  * ---- Global Information --------------------------------------------------------
  * > request count                                       1000 (OK=1000   KO=0     )
  * > min response time                                      1 (OK=1      KO=-     )
  * > max response time                                     20 (OK=20     KO=-     )
  * > mean response time                                     2 (OK=2      KO=-     )
  * > std deviation                                          1 (OK=1      KO=-     )
  * > response time 50th percentile                          2 (OK=2      KO=-     )
  * > response time 75th percentile                          2 (OK=2      KO=-     )
  * > response time 95th percentile                          3 (OK=3      KO=-     )
  * > response time 99th percentile                          6 (OK=6      KO=-     )
  * > mean requests/sec                                333.333 (OK=333.333 KO=-     )
  * ---- Response Time Distribution ------------------------------------------------
  * > t < 800 ms                                          1000 (100%)
  * > 800 ms < t < 1200 ms                                   0 (  0%)
  * > t > 1200 ms                                            0 (  0%)
  * > failed                                                 0 (  0%)
  * ================================================================================
  * }}}
  */
class GatlingPingFloodRequestSimulationIT extends Simulation with Configuration {

  val testHost = config.getString("gatling.test.host")

  val requestCount = 1000

  val httpProtocol = http
    //.baseUrl("http://localhost:8080") // Here is the root for all relative URLs
    .baseUrl(s"http://$testHost") // Here is the root for all relative URLs
    .acceptHeader("text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8") // Here are the common headers
    .acceptEncodingHeader("gzip, deflate")
    .acceptLanguageHeader("en-US,en;q=0.5")
    .userAgentHeader("Mozilla/5.0 (Macintosh; Intel Mac OS X 10.8; rv:16.0) Gecko/20100101 Firefox/16.0")

  val simpleUser = scenario("Ping") // A scenario is a chain of requests and pauses
      .repeat(requestCount) {
          exec(http("ping request").get("/ping"))
        }

  setUp(simpleUser.inject(atOnceUsers(1)).protocols(httpProtocol))
}