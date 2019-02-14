package net.kolotyluk.leaderboard.Akka.endpoint

import akka.http.scaladsl.model.StatusCodes.MethodNotAllowed
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpResponse}
import akka.http.scaladsl.server.{Directives, Route}
import io.swagger.annotations._
import javax.ws.rs.Path


@Api(value = "/ping", produces = "text/plain(UTF-8)")
@Path("/ping")
class PingEndpoint extends Directives {

  val route = ping

  /** =get ping=
    * {{{
    * curl http://localhost:8080/ping
    * }}}
    * @return
    */
  @ApiOperation(value = "Simple keep-alive test", nickname = "ping", httpMethod = "GET", response = classOf[String])
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "Return 'pong' response", response = classOf[String]),
  ))
  def ping : Route =
    path("ping") {
      pathEnd {
        get {
          complete(HttpEntity(ContentTypes.`text/plain(UTF-8)`, "pong"))
        } ~
        // unix:    curl -d "http://localhost:8080/ping"
        // windows: curl -Method Post http://localhost:8080/ping
        // https://restfulapi.net/http-methods
        complete(HttpResponse(MethodNotAllowed, entity = s"only GET method supported"))
      }
    }
}
