package net.kolotyluk.leaderboard.Akka.endpoint

import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.Directives
import io.swagger.annotations._
import javax.ws.rs.Path
import net.kolotyluk.leaderboard.Akka.RestActor.Fail
import net.kolotyluk.leaderboard.Akka.restActor

@Api(value = "/fail", produces = "text/plain(UTF-8)")
@Path("/fail")
class FailEndpoint extends Directives {
//  class FailService(restActorRef: ActorRef[RestActor.Message]) extends Directives {

  val route = fail

  @ApiOperation(value = "Simple keep-alive test", nickname = "fail", httpMethod = "GET", response = classOf[String])
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "Return 'failed' response", response = classOf[String]),
  ))
  def fail =
    path("fail") {
      get {
        restActor.getActorRef() ! Fail(new Exception("BOOM"))
        complete(HttpEntity(ContentTypes.`text/plain(UTF-8)`, "failed"))
      }
    }
}
