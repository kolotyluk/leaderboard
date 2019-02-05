package net.kolotyluk.leaderboard.akka.endpoint.leaderboard

import java.util.UUID

import akka.http.scaladsl.model.StatusCodes.{BadRequest, OK}
import net.kolotyluk.leaderboard.Akka.endpoint.urlIdToInternalIdentifier
import net.kolotyluk.leaderboard.Akka.endpoint.leaderboard.leaderboardEndpoint
import net.kolotyluk.leaderboard.Akka.endpoint.leaderboard.{LeaderboardJsonSupport, LeaderboardPostRequest, LeaderboardPostResponse, LeaderboardStatusResponses}
import unit.RoutingSpec

import scala.language.postfixOps

class LeaderboardEndpointSpec extends RoutingSpec with LeaderboardJsonSupport {

  val createConcurrentLeaderboard = LeaderboardPostRequest(Some("foo"), "ConcurrentLeaderboard")
  val createLeaderboardActor = LeaderboardPostRequest(Some("foo"), "LeaderboardActor")
  val createSynchronizedConcurrentLeaderboard = LeaderboardPostRequest(Some("foo"), "SynchronizedConcurrentLeaderboard")
  val createSynchronizedLeaderboard = LeaderboardPostRequest(Some("foo"), "SynchronizedLeaderboard")
  val createBogusLeaderboard = LeaderboardPostRequest(Some("foo"), "BogusLeaderboard")

  behavior of "/leaderboard Endpoint"

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
      status shouldBe OK
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
      status shouldBe OK
      When("status == OK")
      val response = responseAs[LeaderboardPostResponse]
      urlIdToInternalIdentifier(response.id).getValue[UUID] shouldBe a [UUID]
      Then("response.id parses to a UUID")
      response.name shouldBe Some(name)
      And(s"response.name == $name")
    }
  }

  it should "return a correct response for POST requests to create ConcurrentLeaderboard" in {
    val payload = LeaderboardPostRequest(None, "ConcurrentLeaderboard")
    Post("/leaderboard", payload) ~> leaderboardEndpoint.routes ~> check {
      // TODO figure out how to marshal this to a simple payload string
      Given(s"POST /leaderboard $payload")
      status shouldBe OK
      When("status == OK")
      val response = responseAs[LeaderboardPostResponse]
      urlIdToInternalIdentifier(response.id).getValue[UUID] shouldBe a [UUID]
      Then("response.id parses to a UUID")
      response.name shouldBe None
      And("response.name == None")
    }
  }

  it should "return an error response for POST requests to create leaderboard with  name conflicts" in {
    val payload = LeaderboardPostRequest(Some("foo"), "ConcurrentLeaderboard")
    Post("/leaderboard?name=contest", payload) ~> leaderboardEndpoint.routes ~> check {
      Given(s"POST /leaderboard?name=contest $payload")
      status shouldBe BadRequest
      When(s"status = BadRequest")
      responseAs[String] should startWith ("ambiguous request:")
      Then(s"it failed because it's an ambiguous request")


      //      When("status == OK")
//      val response = responseAs[LeaderboardPostResponse]
//      response.name shouldEqual None
//      Then("response.name == Some(\"contest\")")
//      //UrlIdToUuid(response.id) shouldBe(BeMatcher[UUID])
    }
  }
//
//  it should "return a correct response for POST requests to create LeaderboardActor" in {
//    Post("/leaderboard", createLeaderboardActor) ~> leaderboardEndpoint.routes  ~> check {
//      Given(createLeaderboardActor.toString)
//      status shouldEqual StatusCodes.OK
//      When("status == OK")
//      responseAs[LeaderboardPostResponse].name shouldEqual Some("foo")
//      Then("response.name == foo")
//    }
//  }
//
//  it should "return a correct response for POST requests to create SynchronizedConcurrentLeaderboard" in {
//    Post("/leaderboard", createSynchronizedConcurrentLeaderboard) ~> leaderboardEndpoint.routes  ~> check {
//      Given(createSynchronizedConcurrentLeaderboard.toString)
//      status shouldEqual StatusCodes.OK
//      When("status == OK")
//      responseAs[LeaderboardPostResponse].name shouldEqual Some("foo")
//      Then("response.name == foo")
//    }
//  }
//
//  it should "return a correct response for POST requests to create SynchronizedLeaderboard" in {
//    Post("/leaderboard", createSynchronizedLeaderboard) ~> leaderboardEndpoint.routes  ~> check {
//      Given(createSynchronizedLeaderboard.toString)
//      status shouldEqual StatusCodes.OK
//      When("status == OK")
//      responseAs[LeaderboardPostResponse].name shouldEqual Some("foo")
//      Then("response.name == foo")
//    }
//  }
//
//  it should "return a correct response for POST requests to create BogusLeaderboard" in {
//    Post("/leaderboard", createBogusLeaderboard) ~> leaderboardEndpoint.routes  ~> check {
//      Given(createBogusLeaderboard.toString)
//      status shouldEqual StatusCodes.BadRequest
//      When("status == BadRequest")
//    }
//  }
}
