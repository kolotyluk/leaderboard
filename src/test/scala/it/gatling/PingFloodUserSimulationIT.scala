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
  * > min response time                                      6 (OK=6      KO=-     )
  * > max response time                                   1079 (OK=1079   KO=-     )
  * > mean response time                                   123 (OK=123    KO=-     )
  * > std deviation                                        143 (OK=143    KO=-     )
  * > response time 50th percentile                        119 (OK=119    KO=-     )
  * > response time 75th percentile                        198 (OK=198    KO=-     )
  * > response time 95th percentile                        251 (OK=251    KO=-     )
  * > response time 99th percentile                       1063 (OK=1063   KO=-     )
  * > mean requests/sec                                    500 (OK=500    KO=-     )
  * ---- Response Time Distribution ------------------------------------------------
  * > t < 800 ms                                           986 ( 99%)
  * > 800 ms < t < 1200 ms                                  14 (  1%)
  * > t > 1200 ms                                            0 (  0%)
  * > failed                                                 0 (  0%)
  * ================================================================================
  * }}}
  */
class PingFloodUserSimulationIT extends Simulation with Configuration {

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
    .exec(
      http("ping request")
        .get("/ping"))
        //.pause(7) // Note that Gatling has recorder real time pauses

  setUp(simpleUser.inject(atOnceUsers(requestCount)).protocols(httpProtocol))
}