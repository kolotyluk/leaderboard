package net.kolotyluk.leaderboard.Akka

import akka.http.scaladsl.model.StatusCodes.{BadRequest, MethodNotAllowed, NotFound}
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpResponse}
import akka.http.scaladsl.server.{Directives, Route}
import io.swagger.annotations._
import javax.ws.rs.Path
import net.kolotyluk.leaderboard.scorekeeping.ConcurrentLeaderboard

import scala.util.{Failure, Success}

@Api(value = "/leaderboard", produces = "text/plain(UTF-8)")
@Path("/leaderboard")
class LeaderboardEndpoint extends Directives {

  //val routes = leaderboardRoute

  def routes: Route =
    path("leaderboard" ) {
      parameter('name) { name =>
       pathEnd {
          leaderboardGet(name) ~
          leaderboardPost(name)
        }
      } ~
      pathEnd {
        post {
          complete {
            //HttpEntity(ContentTypes.`text/plain(UTF-8)`, s"Leaderboard query expected")
            s"Leaderboard not created, name missing"
          }
        } ~
        complete {
          //HttpEntity(ContentTypes.`text/plain(UTF-8)`, s"Leaderboard query expected")
          HttpResponse(BadRequest, entity = "****query missing****")
        }
      }
    }

  @Path("?name={name}")
  @ApiOperation(value = "Return info on named leaderboard", notes = "", nickname = "leaderboardGet", httpMethod = "GET")
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "Return leaderboard info", response = classOf[String]),
    new ApiResponse(code = 404, message = "Return leaderboard not found", response = classOf[String])
  ))
  def leaderboardGet(name: String): Route =
    get {
      complete {
        ConcurrentLeaderboard.get(name) match {
          case None =>  HttpResponse(NotFound, entity = HttpEntity(ContentTypes.`text/plain(UTF-8)`, s"Leaderboard '$name' not found"))
          case Some(leaderboard) => HttpEntity(ContentTypes.`text/plain(UTF-8)`, s"Leaderboard '$name' exists")
        }
      }
    }

  /** =POST leaderboard=
    * ===curl examples=
    * {{{
    * unix shell: curl -d "" http://localhost:8080/leaderboard?name=foo
    * PowerShell: curl -Method Post http://localhost:8080/leaderboard?name=foo
    * }}}
    * @param name
    * @return
    */
  @Path("?name={name}")
  @ApiOperation(value = "Return info on named leaderboard", notes = "", nickname = "leaderboardPost", httpMethod = "POST")
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "Return leaderboard created confirmation", response = classOf[String]),
    new ApiResponse(code = 405, message = "Return leaderboard already exists", response = classOf[String])
  ))
  def leaderboardPost(name: String): Route =
    post {
      complete {
        ConcurrentLeaderboard.add(name) match {
          case Failure(cause) => HttpResponse(MethodNotAllowed, entity = s"leaderboard?name=$name already exists")
          case Success(leaderboard) => s"Leaderboard $name created"
        }
      }
    }
}
