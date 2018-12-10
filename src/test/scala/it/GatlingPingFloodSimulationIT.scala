package it

import io.gatling.core.Predef._
import io.gatling.http.Predef._

/** =Basic Gatling Simulation=
  * Ping the leaderboard service
  * <p>
  * usage: `mvn gatling:test`
  * {{{
  * 2 users
  * ================================================================================
  * ---- Global Information --------------------------------------------------------
  * > request count                                       1000 (OK=1000   KO=0     )
  * > min response time                                      1 (OK=1      KO=-     )
  * > max response time                                     26 (OK=26     KO=-     )
  * > mean response time                                     3 (OK=3      KO=-     )
  * > std deviation                                          1 (OK=1      KO=-     )
  * > response time 50th percentile                          3 (OK=3      KO=-     )
  * > response time 75th percentile                          3 (OK=3      KO=-     )
  * > response time 95th percentile                          4 (OK=4      KO=-     )
  * > response time 99th percentile                          5 (OK=5      KO=-     )
  * > mean requests/sec                                    500 (OK=500    KO=-     )
  * ---- Response Time Distribution ------------------------------------------------
  * > t < 800 ms                                          1000 (100%)
  * > 800 ms < t < 1200 ms                                   0 (  0%)
  * > t > 1200 ms                                            0 (  0%)
  * > failed                                                 0 (  0%)
  * ================================================================================
  *
  * 10 users
  * ================================================================================
  * ---- Global Information --------------------------------------------------------
  * > request count                                       1000 (OK=1000   KO=0     )
  * > min response time                                      2 (OK=2      KO=-     )
  * > max response time                                     35 (OK=35     KO=-     )
  * > mean response time                                     5 (OK=5      KO=-     )
  * > std deviation                                          3 (OK=3      KO=-     )
  * > response time 50th percentile                          4 (OK=4      KO=-     )
  * > response time 75th percentile                          5 (OK=5      KO=-     )
  * > response time 95th percentile                          8 (OK=8      KO=-     )
  * > response time 99th percentile                         19 (OK=19     KO=-     )
  * > mean requests/sec                                   1000 (OK=1000   KO=-     )
  * ---- Response Time Distribution ------------------------------------------------
  * > t < 800 ms                                          1000 (100%)
  * > 800 ms < t < 1200 ms                                   0 (  0%)
  * > t > 1200 ms                                            0 (  0%)
  * > failed                                                 0 (  0%)
  * ================================================================================
  * }}}
  */
class GatlingPingFloodSimulationIT extends Simulation {

  val requestCount = 10000
  val userCount = 100

  val httpProtocol = http
    //.baseUrl("http://localhost:8080") // Here is the root for all relative URLs
    .baseUrl("http://192.168.0.19:8080") // Here is the root for all relative URLs
    .acceptHeader("text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8") // Here are the common headers
    .acceptEncodingHeader("gzip, deflate")
    .acceptLanguageHeader("en-US,en;q=0.5")
    .userAgentHeader("Mozilla/5.0 (Macintosh; Intel Mac OS X 10.8; rv:16.0) Gecko/20100101 Firefox/16.0")

  val simpleUser = scenario("Ping") // A scenario is a chain of requests and pauses
      .repeat(requestCount / userCount) {
          exec(http("ping request").get("/ping"))
        }

  setUp(simpleUser.inject(atOnceUsers(userCount)).protocols(httpProtocol))
}