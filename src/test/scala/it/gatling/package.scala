package it

import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

import io.gatling.commons.stats.KO
import io.gatling.core.Predef._
import io.gatling.core.structure.ChainBuilder
import io.gatling.http.Predef._
import net.kolotyluk.leaderboard.akka_specific.endpoint.leaderboard._
import net.kolotyluk.scala.extras.{Configuration, Logging, base64UrlIdToUuid, uuidToBase64UrlId}
import spray.json._

import scala.annotation.tailrec
import scala.collection.concurrent.TrieMap
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.Random

/** =Gatling Simulation Behaviors=
  * Collection of resources (execution chains, scenarios, etc.) for defining simulation setups
  * <p>
  * usage:
  * {{{
  * mvn test-compile
  * mvn gatling:test
  * }}}
  * or
  * {{{
  * `mvn verify
  * }}}
  *
  * =Purpose=
  * The purpose of these tests is to characterize the performance behavior of the system so that design and
  * implementation changes can be measured.
  *
  * ==Test Strategy==
  * Simulate a scoring simulation where leaderboard members can directly add their scores to a leaderboard.
  *
  * ===Test Levels===
  *
  * Logically these are [[https://en.wikipedia.org/wiki/System_testing System Tests]],
  * specifically [[https://en.wikipedia.org/wiki/Software_performance_testing Performance Tests]]
  * and/or [[https://en.wikipedia.org/wiki/Load_testing Load Tests]].
  * <p>
  * Pragmatically these are run as automated [[https://en.wikipedia.org/wiki/Integration_testing Integration Tests]],
  * such as via the [[https://maven.apache.org/surefire/maven-failsafe-plugin Maven Failsafe Plugin]].
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
  * @see [[https://en.wikipedia.org/wiki/Test_strategy Test Strategy]]
  * @see [[https://en.wikipedia.org/wiki/Test_plan Test Plan]]
  * @see [[https://en.wikipedia.org/wiki/Test_suite Test Suite]]
  * @see [[https://en.wikipedia.org/wiki/Test_case Test Case]]
  */
package object gatling extends Simulation with Configuration with Logging with JsonSupport {

  val continue = new AtomicBoolean(true)

  val userIdToUrlId = new TrieMap[Long,String]()

  def memberSeq(count: Int): IndexedSeq[String] = {
    @tailrec def memberSeq(count: Int, result: Vector[String]): Vector[String] =
      if (count > 0) memberSeq(count - 1, result :+ uuidToBase64UrlId(UUID.randomUUID())) else result
    memberSeq(count, Vector.empty[String])
  }

  val members = memberSeq(100)

  var leaderboardId = ""

  val testHost = config.getString("gatling.test.host")

  val httpProtocol = http
    //.baseUrl("http://localhost:8080") // Here is the root for all relative URLs
    .baseUrl(s"http://$testHost") // Here is the root for all relative URLs
    .acceptHeader("text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8") // Here are the common headers
    .acceptEncodingHeader("gzip, deflate")
    .acceptLanguageHeader("en-US,en;q=0.5")
    .userAgentHeader("Mozilla/5.0 (Macintosh; Intel Mac OS X 10.8; rv:16.0) Gecko/20100101 Firefox/16.0")

  val httpUpdateProtocol = http
    //.baseUrl("http://localhost:8080") // Here is the root for all relative URLs
    .baseUrl(s"http://$testHost") // Here is the root for all relative URLs
    .acceptHeader("text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8") // Here are the common headers
    .acceptLanguageHeader("en-US,en;q=0.5")
    //.userAgentHeader("Mozilla/5.0 (Macintosh; Intel Mac OS X 10.8; rv:16.0) Gecko/20100101 Firefox/16.0")

  def createLeaderboardChain(implementation: String) = exec(http("Create leaderboard")
    .post("/leaderboard").body(StringBody(session => {
      val body = LeaderboardPostRequest(None, implementation)
      body.toJson.compactPrint
    })).asJson
    .check(status.is(201),
      jsonPath("$..id").ofType[String].saveAs("leaderboardId")
    )
  )

  val updateLeaderboardChain = exec(http("Update leaderboard")
    .patch("/leaderboard/${leaderboardId}/${memberId}?score=10")
    .check(status.is(200))
  )

  def bulkUpdateLeaderboardChain(size: Int) = exec(http("Bulk Update leaderboard")
    .post("/leaderboard").body(StringBody(session => {
      val body = UpdateScoresRequest(
        Seq(LeaderboardScores(leaderboardId,
          for {
            _ <- 1 to size
            member = members(Random.nextInt(members.size))
          } yield MemberScore(member, Random.nextInt.toString, None)
        )))
      body.toJson.compactPrint
    })).asJson).exitHereIfFailed

  def createLeaderboardScenario(implementation: String) = scenario("Create scenario")
    .exec(createLeaderboardChain(implementation))
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
    .repeat(150) {
      exec(updateLeaderboardChain)
    }

  def us(chainBuilder: ChainBuilder, repeat: Int) = scenario("Update scenario")
    .exec(
      doIf(session => continue.get) {
        exec{ session =>
          val userId = session.userId
          val memberId = userIdToUrlId.getOrElseUpdate(userId, uuidToBase64UrlId(UUID.randomUUID()))
          val uuid = base64UrlIdToUuid(memberId)
          logger.debug(s"userId = $userId, leaderboardId = $leaderboardId, memberId = $memberId, uuid = $uuid")
          session
            .set("leaderboardId", leaderboardId)
            .set("memberId", memberId)
        }
      }
      .repeat(repeat) {
        doIf(session => continue.get) {
          exec((session: io.gatling.core.session.Session) => {
            if (session.status == KO) {
              continue.set(false)
            } else exec (chainBuilder)
            session
          })
        }
      }
    )

  def updateScenario(chainBuilder: ChainBuilder, repeat: Int) = scenario("Update scenario")
    .exec{ session =>
      val userId = session.userId
      val memberId = userIdToUrlId.getOrElseUpdate(userId, uuidToBase64UrlId(UUID.randomUUID()))
      val uuid = base64UrlIdToUuid(memberId)
      logger.debug(s"userId = $userId, leaderboardId = $leaderboardId, memberId = $memberId, uuid = $uuid")
      session
        .set("leaderboardId", leaderboardId)
        .set("memberId", memberId)
    }
    .repeat(repeat) {
      exec(chainBuilder)
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

  def updateSetUp(users: Int, size: Int, implementation: String) = setUp(
    createLeaderboardScenario(implementation).inject(
      atOnceUsers(1))
      .protocols(httpProtocol),
    updateScenario(bulkUpdateLeaderboardChain(size), 50).inject(
      nothingFor(4 seconds),
      rampUsers(users) during (100 seconds))
      .protocols(httpUpdateProtocol)
  )
}
