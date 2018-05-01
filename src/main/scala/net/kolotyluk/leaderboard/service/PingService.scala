package net.kolotyluk.leaderboard.service

import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.Directives
import akka.http.scaladsl.server.Directives.{complete, get, path}

import javax.ws.rs.Path
import io.swagger.annotations._

@Api(value = "/ping", produces = "text/plain(UTF-8)")
@Path("/ping")
object PingService extends Directives {

  val route = ping

  @ApiOperation(value = "Simple keep-alive test", nickname = "ping", httpMethod = "GET", response = classOf[String])
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "Return 'pong' response", response = classOf[String]),
  ))
  def ping =
    path("ping") {
      get {
        complete(HttpEntity(ContentTypes.`text/plain(UTF-8)`, "pong"))
      }
    }
}
