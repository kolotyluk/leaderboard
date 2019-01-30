package net.kolotyluk.leaderboard.akka

import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import net.kolotyluk.leaderboard.Akka.endpoint.pingEndpoint
import unit.UnitSpec
import akka.http.scaladsl.model.StatusCodes

import scala.language.postfixOps

class PingEndpointSpec extends UnitSpec with ScalatestRouteTest  {

  behavior of "PingEndpoint"

  it should "return a 'pong' response for GET requests to /ping" in {
    Get("/ping") ~> pingEndpoint.route  ~> check {
      responseAs[String] shouldEqual "pong"
    }
  }

  it should "leave GET requests to other paths unhandled" in {
    Get("/kermit") ~> pingEndpoint.route ~> check {
      handled shouldBe false
    }
  }

  it should "return a MethodNotAllowed error for PUT requests" in {
    Put("/ping") ~> Route.seal(pingEndpoint.route) ~> check {
      status shouldEqual StatusCodes.MethodNotAllowed
      responseAs[String] shouldEqual "only GET method supported"
    }
  }
}
