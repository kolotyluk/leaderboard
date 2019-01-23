package net.kolotyluk.leaderboard.akka

import akka.http.scaladsl.model.StatusCodes
import net.kolotyluk.leaderboard.Akka.{LeaderboardJsonSupport, LeaderboardPostRequest, LeaderboardPostResponse, leaderboardEndpoint}
import unit.RoutingSpec

import scala.language.postfixOps

class LeaderboardEndpointSpec extends RoutingSpec with LeaderboardJsonSupport {

  behavior of "LeaderboardEndpoint"

  val createConcurrentLeaderboard = LeaderboardPostRequest("foo", "ConcurrentLeaderboard")
  val createLeaderboardActor = LeaderboardPostRequest("foo", "LeaderboardActor")
  val createSynchronizedConcurrentLeaderboard = LeaderboardPostRequest("foo", "SynchronizedConcurrentLeaderboard")
  val createSynchronizedLeaderboard = LeaderboardPostRequest("foo", "SynchronizedLeaderboard")
  val createBogusLeaderboard = LeaderboardPostRequest("foo", "BogusLeaderboard")

  it should "return a correct response for POST requests to create ConcurrentLeaderboard" in {
    Post("/leaderboard", createConcurrentLeaderboard) ~> leaderboardEndpoint.routes  ~> check {
      Given(createConcurrentLeaderboard.toString)
      status shouldEqual StatusCodes.OK
      When("status == OK")
      responseAs[LeaderboardPostResponse].name shouldEqual Some("foo")
      Then("response.name == foo")
    }
  }

  it should "return a correct response for POST requests to create LeaderboardActor" in {
    Post("/leaderboard", createLeaderboardActor) ~> leaderboardEndpoint.routes  ~> check {
      Given(createLeaderboardActor.toString)
      status shouldEqual StatusCodes.OK
      When("status == OK")
      responseAs[LeaderboardPostResponse].name shouldEqual Some("foo")
      Then("response.name == foo")
    }
  }

  it should "return a correct response for POST requests to create SynchronizedConcurrentLeaderboard" in {
    Post("/leaderboard", createSynchronizedConcurrentLeaderboard) ~> leaderboardEndpoint.routes  ~> check {
      Given(createSynchronizedConcurrentLeaderboard.toString)
      status shouldEqual StatusCodes.OK
      When("status == OK")
      responseAs[LeaderboardPostResponse].name shouldEqual Some("foo")
      Then("response.name == foo")
    }
  }

  it should "return a correct response for POST requests to create SynchronizedLeaderboard" in {
    Post("/leaderboard", createSynchronizedLeaderboard) ~> leaderboardEndpoint.routes  ~> check {
      Given(createSynchronizedLeaderboard.toString)
      status shouldEqual StatusCodes.OK
      When("status == OK")
      responseAs[LeaderboardPostResponse].name shouldEqual Some("foo")
      Then("response.name == foo")
    }
  }

  it should "return a correct response for POST requests to create BogusLeaderboard" in {
    Post("/leaderboard", createBogusLeaderboard) ~> leaderboardEndpoint.routes  ~> check {
      Given(createBogusLeaderboard.toString)
      status shouldEqual StatusCodes.BadRequest
      When("status == BadRequest")
    }
  }
}
