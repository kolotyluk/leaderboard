package it

import java.util.UUID

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import net.kolotyluk.scala.extras.{Configuration, Logging, base64UrlIdToUuid, uuidToBase64UrlId}

import scala.collection.concurrent.TrieMap

package object gatling extends Simulation with Configuration with Logging {

  val userIdToUrlId = new TrieMap[Long,String]()

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

  val updateLeaderboardChain = exec(http("Update leaderboard")
    .patch("/leaderboard/${leaderboardId}/${memberId}?score=10")
    .check(status.is(200)))

  val createLeaderboardScenario = scenario("Create scenario")
    .exec(createLeaderboardChain)
    .exec {session =>
      leaderboardId = session("leaderboardId").as[String]
      logger.info(s"leaderboardId = $leaderboardId")
      session
    }

  /** =Update Leaderboard With Scores=
    *
    * ==Test Case==
    *
    * For each user: update the leaderboard, multiple times, with some score, as if participating in some contest or
    * event in a game.
    *
    *
    */
  val updateLeaderboardScenario = scenario("Update scenario")
    .exec{ session =>
      val userId = session.userId
      val memberId = userIdToUrlId.getOrElseUpdate(userId, uuidToBase64UrlId(UUID.randomUUID()))
      val uuid = base64UrlIdToUuid(memberId)
      logger.debug(s"userId = $userId, leaderboardId = $leaderboardId, memberId = $memberId, uuid = $uuid")
      session
        .set("leaderboardId", leaderboardId)
        .set("memberId", memberId)
    }
    .repeat(100) {
      exec(updateLeaderboardChain)
    }

  val pingScenario = scenario("Ping") // A scenario is a chain of requests and pauses
    .repeat(1) {
    exec(http("ping request").get("/ping"))
  }

  val requestCount = 100000
  val userCount = 1000

  val pingFloodScenario = scenario("Ping") // A scenario is a chain of requests and pauses
    .repeat(requestCount / userCount) {
    exec(http("ping request").get("/ping"))
  }
}
