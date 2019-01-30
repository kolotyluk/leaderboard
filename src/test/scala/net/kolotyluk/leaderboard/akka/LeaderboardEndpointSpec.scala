package net.kolotyluk.leaderboard.akka

import akka.http.scaladsl.model.StatusCodes
import net.kolotyluk.leaderboard.Akka.endpoint.{LeaderboardJsonSupport, LeaderboardPostRequest, LeaderboardPostResponse, leaderboardEndpoint}
import unit.RoutingSpec

import scala.language.postfixOps

class LeaderboardEndpointSpec extends RoutingSpec with LeaderboardJsonSupport {

  val createConcurrentLeaderboard = LeaderboardPostRequest(Some("foo"), "ConcurrentLeaderboard")
  val createLeaderboardActor = LeaderboardPostRequest(Some("foo"), "LeaderboardActor")
  val createSynchronizedConcurrentLeaderboard = LeaderboardPostRequest(Some("foo"), "SynchronizedConcurrentLeaderboard")
  val createSynchronizedLeaderboard = LeaderboardPostRequest(Some("foo"), "SynchronizedLeaderboard")
  val createBogusLeaderboard = LeaderboardPostRequest(Some("foo"), "BogusLeaderboard")

  behavior of "LeaderboardEndpoint"

  it should "return a correct response for POST requests to create default leaderboard" in {
    Post("/leaderboard") ~> leaderboardEndpoint.routes  ~> check {
      Given("POST /leaderboard")
      status shouldEqual StatusCodes.OK
      When("status == OK")
      responseAs[LeaderboardPostResponse].name shouldEqual None
      Then("response.name == None")
    }
  }

  it should "return a correct response for POST requests to create ConcurrentLeaderboard" in {
    Post("/leaderboard", createConcurrentLeaderboard) ~> leaderboardEndpoint.routes ~> check {
      Given(s"POST /leaderboard ${createConcurrentLeaderboard.toString}")
      status shouldEqual StatusCodes.OK
      When("status == OK")
      val response = responseAs[LeaderboardPostResponse]
      And(s"response.name = ${response.name}")
      response.name shouldEqual Some("foo")
      Then("response.name == Some(\"foo\")")
    }
  }

  it should "return an error response for POST requests to create ConcurrentLeaderboard named 'contest'" in {
    Post("/leaderboard?name=contest", createConcurrentLeaderboard) ~> leaderboardEndpoint.routes ~> check {
      Given(s"POST /leaderboard?name=contest ${createConcurrentLeaderboard.toString}")
      status shouldEqual StatusCodes.BadRequest
      When(s"status = ${StatusCodes.BadRequest}")
      Then(s"response = $response")
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
