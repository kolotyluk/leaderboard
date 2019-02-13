package net.kolotyluk.leaderboard.akka.endpoint.leaderboard

import java.util.UUID

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.http.scaladsl.model.StatusCodes.{BadRequest, Created, NotFound, OK}
import net.kolotyluk.leaderboard.Akka.endpoint.leaderboard._
import net.kolotyluk.leaderboard.Akka.endpoint.urlIdToInternalIdentifier
import net.kolotyluk.leaderboard.Akka.guardianActor
import unit.RoutingSpec

import scala.language.postfixOps

/** =Leaderboard Endpoint Unit Test Specification=
  *
  */
class EndpointSpec extends RoutingSpec with Behaviors with JsonSupport {

  // For this test suite we start a full actor system. Unlike the main service, however, we do not bind to
  // a socket listening for incoming HTTP sessions, as the endpoints are tested via the test harness.
  val testKit = ActorTestKit()

  override def afterAll(): Unit = {
    testKit.shutdownTestKit()
  }

  val guardianActorRef = testKit.spawn(guardianActor.behavior)
  // Normally we would send a Bind message to the guardian, but we're only unit testing, not integration testing

  behavior of "/leaderboard Endpoint"

  it should "return NotFound for unknown leaderboard" in {
    val uri = "/leaderboard/IMvWdANITIWZxm7efUKVAg"
    Get(uri) ~> leaderboardEndpoint.routes  ~> check {
      Given(s"GET $uri")
      status shouldBe NotFound
      Then("status shouldBe NotFound")
      When(s"response=${response.entity}")
      // TODO check that response.explanation URI exists
      // TODO check system log that response.systemLogMessage exists
    }
  }

  it should "return a correct response for minimal GET requests" in {
    val uri = "/leaderboard"
    Get(uri) ~> leaderboardEndpoint.routes  ~> check {
      Given(s"GET $uri")
      status shouldBe OK
      When("status == OK")
      responseAs[LeaderboardStatusResponses].leaderboards.size shouldEqual 0
      Then("response.leaderboards.size == 0")
    }
  }

  it should "return a correct response for minimal POST requests" in {
    val uri = "/leaderboard"
    Post(uri) ~> leaderboardEndpoint.routes  ~> check {
      Given(s"POST $uri")
      status shouldBe Created
      When("status == OK")
      val response = responseAs[LeaderboardPostResponse]
      urlIdToInternalIdentifier(response.id).getValue[UUID] shouldBe a [UUID]
      Then("response.id parses to a UUID")
      response.name shouldBe None
      And("response.name == None")
    }

    val name = "foo"

    val nameUri = s"/leaderboard?name=$name"
    Post(nameUri) ~> leaderboardEndpoint.routes  ~> check {
      Given(s"POST $nameUri")
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
    val uri = "/leaderboard"
    val createBogusLeaderboard = LeaderboardPostRequest(Some("foo"), "BogusLeaderboard")
    Post(uri, createBogusLeaderboard) ~> leaderboardEndpoint.routes  ~> check {
      Given(s"POST $uri ${createBogusLeaderboard.toString}")
      status shouldBe BadRequest
      When("status == BadRequest")
      When(s"response=${response.entity}")
      // TODO check that response.explanation URI exists
      // TODO check system log that response.systemLogMessage exists
    }
  }

  Implementation.values.foreach{implementation =>

    it must behave like verifyPostRequests(implementation)

  }

  Implementation.values.foreach{implementation =>

    it must behave like verifyScoreRequest(implementation)

  }

}
