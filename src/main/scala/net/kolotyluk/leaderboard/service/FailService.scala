package net.kolotyluk.leaderboard.service

import akka.actor.typed.ActorRef
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.Directives
import io.swagger.annotations._
import javax.ws.rs.Path
import net.kolotyluk.leaderboard.actor.Rest
import net.kolotyluk.leaderboard.actor.Rest.Fail

@Api(value = "/ping", produces = "text/plain(UTF-8)")
@Path("/ping")
class FailService(restActorRef: ActorRef[Rest.Message]) extends Directives {

  val route = fail

  @ApiOperation(value = "Simple keep-alive test", nickname = "ping", httpMethod = "GET", response = classOf[String])
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "Return 'pong' response", response = classOf[String]),
  ))
  def fail =
    path("fail") {
      get {
        restActorRef ! Fail(new Exception("BOOM"))
        complete(HttpEntity(ContentTypes.`text/plain(UTF-8)`, "failed"))
      }
    }
}
