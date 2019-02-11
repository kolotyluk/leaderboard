package net.kolotyluk.leaderboard.akka.endpoint.leaderboard

import java.util.UUID

import akka.http.scaladsl.model.StatusCodes.{BadRequest, Created}
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

    def handle(accept: Boolean, queryName: Option[String], payloadName: Option[String]) = {

      val endpoint = queryName match {
        case None => "/leaderboard"
        case Some(value) => s"/leaderboard?name=$value"
      }

      val payload = LeaderboardPostRequest(payloadName, implementation.toString)

      val leaderboardName = queryName match {
        case None => payloadName
        case Some(_) => queryName
      }

      Post(endpoint, payload) ~> leaderboardEndpoint.routes ~> check {
        // TODO figure out how to marshal this to a simple payload string
        Given(s"POST $endpoint $payload")
        accept match {
          case true =>
            status shouldBe Created
            When("status == OK")
            val response = responseAs[LeaderboardPostResponse]
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
        }
      }
    }

    it should s"return a correct response for POST requests to create ${implementation.toString}" in {
      // We need to synthesize some unique names
      val thing1 = Some("thing" + Random.nextLong())
      val thing2 = Some("thing" + Random.nextLong())
      val thing3 = Some("thing" + Random.nextLong())
      val thing4 = Some("thing" + Random.nextLong())
      handle(true,  None, None)
      handle(true,  None, thing1)
      handle(true,  thing2, thing2)
      handle(true,  thing3, None)
      handle(false, thing4, Some("conflict"))
    }
  }

}