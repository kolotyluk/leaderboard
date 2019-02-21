package it
import io.gatling.core.Predef._
import io.gatling.http.Predef._
import net.kolotyluk.scala.extras.Configuration

package object gatling extends Simulation with  Configuration {

  var leaderboardId = ""

  val testHost = config.getString("gatling.test.host")

  val httpProtocol = http
    //.baseUrl("http://localhost:8080") // Here is the root for all relative URLs
    .baseUrl(s"http://$testHost") // Here is the root for all relative URLs
    .acceptHeader("text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8") // Here are the common headers
    .acceptEncodingHeader("gzip, deflate")
    .acceptLanguageHeader("en-US,en;q=0.5")
    .userAgentHeader("Mozilla/5.0 (Macintosh; Intel Mac OS X 10.8; rv:16.0) Gecko/20100101 Firefox/16.0")

  val createLeaderboardChain = exec(http("Create leaderboard")
    .post("/leaderboard")
    .check(status.is(201), jsonPath("$..id").ofType[String].saveAs("leaderboardId")))

  val updateLeaderboardChain = exec(http("Update leaderboard").patch("/leaderboard/${leaderboardId}/keAoZQECSwm0h7v6yw_3WQ?score=10"))

  val createLeaderboardScenario = scenario("Create scenario")
    .exec(createLeaderboardChain)
    .exec {session =>
      leaderboardId = session("leaderboardId").as[String]
      println(s"leaderboardId = $leaderboardId")
      session
    }

  val updateLeaderboardScenario = scenario("Update scenario")
    .exec{ session =>
      println(s"leaderboardId = $leaderboardId")
      session.set("leaderboardId", leaderboardId)
    }
    .exec(updateLeaderboardChain)
}
