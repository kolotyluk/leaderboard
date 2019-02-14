package net.kolotyluk.leaderboard.akka.endpoint.leaderboard

import java.util.UUID

import akka.http.scaladsl.model.StatusCodes.{BadRequest, Created, OK}
import net.kolotyluk.leaderboard.Akka.endpoint.leaderboard._
import net.kolotyluk.leaderboard.Akka.endpoint.urlIdToInternalIdentifier
import net.kolotyluk.scala.extras.{Internalized, Logging}
import unit.RoutingSpec

import scala.language.postfixOps
import scala.util.Random


/** =Leaderboard Endpoint Unit Test Behaviors=
  * Standard behaviors of the Leaderboard HTTP Endpoint for all implementations
  * <p>
  * Example use:
  * {{{
  * it must behave like verifyPostRequests(implementation)
  * }}}
  *
  * @param this test specification type
  */
trait Behaviors extends JsonSupport with Logging { this: RoutingSpec =>

  val joeBlow = Internalized(UUID.fromString("02b9670c-afd4-4c39-b4d3-3c46ac4f1a9c"))
  val janeBlow = Internalized(UUID.fromString("02b9670c-afd4-4c39-b4d3-3c46ac4f1a9d"))

  /** =Create New Leaderboard=
    *
    * @param implementation
    */
  def verifyPostRequests(implementation: => Implementation.Value ) {

    def handle(accept: Boolean, queryName: Option[String], payloadName: Option[String]): String = {

      val endpoint = queryName match {
        case None => "/leaderboard"
        case Some(value) => s"/leaderboard?name=$value"
      }

      val payload = LeaderboardPostRequest(payloadName, implementation.toString)

      val leaderboardName = queryName match {
        case None => payloadName
        case Some(_) => queryName
      }

      var response: LeaderboardPostResponse = null

      Post(endpoint, payload) ~> leaderboardEndpoint.routes ~> check {
        // TODO figure out how to marshal this to a simple payload string
        Given(s"POST $endpoint $payload")
        accept match {
          case true =>
            status shouldBe Created
            When("status == Created")
            response = responseAs[LeaderboardPostResponse]
            urlIdToInternalIdentifier(response.id).getValue[UUID] shouldBe a [UUID]
            Then("response.id parses to a UUID")
            response.name shouldBe leaderboardName
            And(s"response.name == $leaderboardName")
          case false =>
            Given(s"POST /leaderboard?name=contest $payload")
            status shouldBe BadRequest
            When(s"status = BadRequest")
            responseAs[String] should startWith ("ambiguous request:")
            Then(s"it failed because it's an ambiguous request")
            response = LeaderboardPostResponse(None, "")
        }
      }

      response.id
    }

    it should s"return a correct response for POST requests to create ${implementation.toString}" in {
      // We need to synthesize some unique names
      val thing1 = Some("thing" + Random.nextLong())
      val thing2 = Some("thing" + Random.nextLong())
      val thing3 = Some("thing" + Random.nextLong())
      val thing4 = Some("thing" + Random.nextLong())

      // Collect the ids of the various implementations of leaderboard
      handle(true,  None, None)
      handle(true,  None, thing1)
      handle(true,  thing2, thing2)
      handle(true,  thing3, None)
      handle(false, thing4, Some("conflict"))
    }
  }

  def verifyScoreRequest(implementation: => Implementation.Value ): Unit = {

    var leaderboardUrlId = ""
    var member1 = "zjR9rwMITES9h5A_2gkJNA"

    it should s"create a ${implementation.toString} leaderboard for score verification" in {
      val url = "/leaderboard"
      val payload = LeaderboardPostRequest(None, implementation.toString)

      leaderboardUrlId = Post(url, payload) ~> leaderboardEndpoint.routes ~> check[String] {
        Given(s"POST $url $payload")
        status shouldBe Created
        When("status == Created")
        val response = responseAs[LeaderboardPostResponse]
        Then(s"leaderboard id = ${response.id}")
        response.id
      }
    }

    it should s"verify a ${implementation.toString} leaderboard was created correctly" in {
      val url = s"/leaderboard/$leaderboardUrlId"
      Get(url) ~> leaderboardEndpoint.routes ~> check {
        Given(s"GET $url")
        status shouldBe OK
        When("status == Ok")
        val response = responseAs[LeaderboardStatusResponse]
        Then( s"response = $response")
        response.size should be (0)
        And("response.size should be (0)")
      }
    }

    it should s"increment a score for a member not yet on the ${implementation.toString} leaderboard" in {
      // Create a score bigger than Long.MaxValue verify scores really are BigInt
      val scoreValue = BigInt(Long.MaxValue) * BigInt(Long.MaxValue)
      val url = s"/leaderboard/$leaderboardUrlId/$member1?score=$scoreValue"
      Patch(url) ~> leaderboardEndpoint.routes ~> check {
        Given(s"PATCH $url")
        status shouldBe OK
        When("status == Ok")
        val response = responseAs[MemberStatusResponse]
        Then( s"response = $response")
        response.leaderboardId should be (leaderboardUrlId)
        And(s"response.leaderboardId should be $leaderboardUrlId")
        response.memberId should be (member1)
        And(s"response.memberId should be $member1")
        response.score match {
          case None =>
            fail
          case Some(score) =>
            BigInt(score.value) should be (scoreValue)
            And(s"response.score should be $score")
        }
      }
    }
  }

}