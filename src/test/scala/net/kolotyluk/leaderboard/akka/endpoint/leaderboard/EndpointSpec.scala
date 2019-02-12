package net.kolotyluk.leaderboard.akka.endpoint.leaderboard

import java.util.UUID

import akka.http.scaladsl.model.StatusCodes.{BadRequest, Created, NotFound, OK}
import net.kolotyluk.leaderboard.Akka.endpoint.leaderboard._
import net.kolotyluk.leaderboard.Akka.endpoint.urlIdToInternalIdentifier
import unit.RoutingSpec

import scala.language.postfixOps

class EndpointSpec extends RoutingSpec with Behaviors with JsonSupport {

  val createConcurrentLeaderboard = LeaderboardPostRequest(Some("foo"), "ConcurrentLeaderboard")
  val createLeaderboardActor = LeaderboardPostRequest(Some("foo"), "LeaderboardActor")
  val createSynchronizedConcurrentLeaderboard = LeaderboardPostRequest(Some("foo"), "SynchronizedConcurrentLeaderboard")
  val createSynchronizedLeaderboard = LeaderboardPostRequest(Some("foo"), "SynchronizedLeaderboard")
  val createBogusLeaderboard = LeaderboardPostRequest(Some("foo"), "BogusLeaderboard")

  behavior of "/leaderboard Endpoint"

  it should "should return not found for unknown leaderboard" in {
    Get("/leaderboard/IMvWdANITIWZxm7efUKVAg") ~> leaderboardEndpoint.routes  ~> check {
      Given("GET /leaderboard/IMvWdANITIWZxm7efUKVAg")
      status shouldBe NotFound
      Then("status shouldBe NotFound")
      When(s"response=${response.entity}")
    }
  }

  it should "return a correct response for minimal GET requests" in {
    Get("/leaderboard") ~> leaderboardEndpoint.routes  ~> check {
      Given("GET /leaderboard")
      status shouldBe OK
      When("status == OK")
      responseAs[LeaderboardStatusResponses].leaderboards.size shouldEqual 0
      Then("response.leaderboards.size == 0")
    }
  }

  it should "return a correct response for minimal POST requests" in {
    Post("/leaderboard") ~> leaderboardEndpoint.routes  ~> check {
      Given("POST /leaderboard")
      status shouldBe Created
      When("status == OK")
      val response = responseAs[LeaderboardPostResponse]
      urlIdToInternalIdentifier(response.id).getValue[UUID] shouldBe a [UUID]
      Then("response.id parses to a UUID")
      response.name shouldBe None
      And("response.name == None")
    }

    val name = "foo"

    Post(s"/leaderboard?name=$name") ~> leaderboardEndpoint.routes  ~> check {
      Given(s"POST /leaderboard?name=$name")
      status shouldBe Created
      When("status == OK")
      val response = responseAs[LeaderboardPostResponse]
      urlIdToInternalIdentifier(response.id).getValue[UUID] shouldBe a [UUID]
      Then("response.id parses to a UUID")
      response.name shouldBe Some(name)
      And(s"response.name == $name")
    }
  }

  it should "return a BadRequest response for requests to create a BogusLeaderboard" in {
    Post("/leaderboard", createBogusLeaderboard) ~> leaderboardEndpoint.routes  ~> check {
      Given(createBogusLeaderboard.toString)
      status shouldBe BadRequest
      When("status == BadRequest")
    }
  }

  Implementation.values.foreach{implementation =>

    it must behave like verifyPostRequests(implementation)

    // it must behave like verifyScoreRequest(implementation)

  }

  Implementation.values.foreach{implementation =>

    // it must behave like verifyPostRequests(implementation)

    it must behave like verifyScoreRequest(implementation)

  }

}
