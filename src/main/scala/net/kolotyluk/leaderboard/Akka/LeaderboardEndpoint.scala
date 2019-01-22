package net.kolotyluk.leaderboard.Akka

import java.util
import java.util.UUID
import java.util.concurrent.{ConcurrentHashMap, ConcurrentSkipListMap}

import akka.event.Logging
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpResponse}
import akka.http.scaladsl.server.{Directives, MalformedRequestContentRejection, RejectionHandler, RequestEntityExpectedRejection, Route, UnsupportedRequestContentTypeRejection}
import io.swagger.annotations._
import javax.ws.rs.Path
import net.kolotyluk.leaderboard.scorekeeping.{ConcurrentLeaderboard, ConsecutiveLeaderboard, Score, SynchronizedConcurrentLeaderboard, SynchronizedLeaderboard}
import net.kolotyluk.scala.extras.Logging
import spray.json.DefaultJsonProtocol

abstract class LeaderboardException extends Exception {
  def getHttpResponse: HttpResponse
}

class DuplicateNameException(name: String) extends LeaderboardException {
  def getHttpResponse() : HttpResponse= {
    HttpResponse(BadRequest, entity = s"leaderboard?name=$name already exists")
  }
}

class UknownKindException(kind: String) extends LeaderboardException {
  def getHttpResponse() : HttpResponse= {
    HttpResponse(BadRequest, entity = s"leaderboard.kind=$kind unknown! Specify one of: ConcurrentLeaderboard, LeaderboardActor, SynchronizedConcurrentLeaderboard, SynchronizedLeaderboard")
  }
}

final case class LeaderboardGetResponse(name: String, state: String, members: Long)

final case class LeaderboardPostRequest(name: String, kind: String)

final case class LeaderboardPostResponse(name: Option[String], id: String)

trait JsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val requestFormat = jsonFormat2(LeaderboardPostRequest)
  implicit val responseFormat = jsonFormat2(LeaderboardPostResponse)
}

@Api(value = "/leaderboard", produces = "text/plain(UTF-8)")
@Path("/leaderboard")
class LeaderboardEndpoint extends Directives with JsonSupport with Logging {

  //val routes = leaderboardRoute

  def routes: Route =
    path("leaderboard" ) {
      parameter('name) { name =>
       pathEnd {
          leaderboardGet(name) ~
          leaderboardPost(Some(name))
        }
      } ~
      pathEnd {
        leaderboardPost(None) //~
//        complete {
//          //HttpEntity(ContentTypes.`text/plain(UTF-8)`, s"Leaderboard query expected")
//          HttpResponse(BadRequest, entity = "****query missing****")
//        }
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

  val postBodyRejections = RejectionHandler.newBuilder().handle{
      case RequestEntityExpectedRejection =>
        complete(BadRequest, "RequestEntityExpectedRejection") // TODO there seems to be no way to get here?
      case UnsupportedRequestContentTypeRejection(supported) =>
        complete(BadRequest, s"UnsupportedRequestContentTypeRejection, expecting one of: ${supported.mkString(", ")}")
      case MalformedRequestContentRejection(message, cause) =>
        logger.error(cause)
        complete(BadRequest, s"MalformedRequestContentRejection $message")
    }.result()

  /** =POST leaderboard=
    * ===curl examples===
    * {{{
    * unix shell: curl -d "" http://localhost:8080/leaderboard?name=foo
    * unix shell: curl -H "Content-Type: application/json" -d '{"name":"foo","kind":"ConcurrentLeaderboard"}' -X POST http://localhost:8080/leaderboard
    * PowerShell: curl -Method Post http://localhost:8080/leaderboard?name=foo
    * PowerShell: Invoke-WebRequest -Method Post http://localhost:8080/leaderboard?name=foo
    * PowerShell: Invoke-WebRequest http://localhost:8080/leaderboard -Method Post -ContentType "application/json" -Body '{"name":"foo","kind":"ConcurrentLeaderboard"}'
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
  def leaderboardPost(name: Option[String]): Route =
    post {
      logRequest("leaderboard", Logging.DebugLevel) {
        handleRejections(postBodyRejections) {
          entity(as[LeaderboardPostRequest]) { leaderboard =>
            try {
              complete(leaderboardCreate(Some(leaderboard.name), Some(leaderboard.kind)))
            } catch {
              case cause: LeaderboardException => complete(cause.getHttpResponse)
              case cause: Throwable =>
                complete(HttpResponse(InternalServerError, entity = s"Exception thrown from LeaderboardPost: ${cause.getMessage}"))
            }
          } //~ complete(HttpResponse(BadRequest, entity = "****body missing****"))
          //} //~
            // complete(concurrentLeaderboard(None))
            //complete(HttpResponse(BadRequest, entity = "****body missing****"))
        } // ~ complete(HttpResponse(BadRequest, entity = "****body missing****"))
      }

//      complete {
//        ConcurrentLeaderboard.add(name) match {
//          case Failure(cause) => HttpResponse(MethodNotAllowed, entity = s"leaderboard?name=$name already exists")
//          case Success(leaderboard) => s"Leaderboard $name created"
//        }
//      }
    }

  def leaderboardCreate(nameOption: Option[String], kindOption: Option[String]): LeaderboardPostResponse = {
    def create(): LeaderboardPostResponse = {
      kindOption match {
        case None | Some("LeaderboardActor") =>
          leaderboardActor(nameOption)
        case Some("ConcurrentLeaderboard") =>
          concurrentLeaderboard(nameOption)
        case Some("SynchronizedConcurrentLeaderboard") =>
          synchronizedConcurrentLeaderboard(nameOption)
        case Some("SynchronizedLeaderboard") =>
          synchronizedLeaderboard(nameOption)
        case Some(kind) =>
          throw new UknownKindException(kind)
      }
    }

    nameOption match {
      case None =>
        create()
      case Some(name) =>
        if (nameToUuid.contains(name)) {
          throw new DuplicateNameException(name)
        } else create()
    }
  }

  def concurrentLeaderboard(nameOption: Option[String]) : LeaderboardPostResponse = {
    val memberToScore = new ConcurrentHashMap[String,Option[Score]]
    val scoreToMember = new ConcurrentSkipListMap[Score,String]
    val leaderboard = new ConcurrentLeaderboard(memberToScore, scoreToMember)
    val uuid = UUID.randomUUID()
    uuidToLeaderboard.put(uuid,leaderboard) match {
      case None =>
        LeaderboardPostResponse(nameOption, uuid.toString)
      case Some(item) =>
        throw new InternalError()
    }
  }

  def leaderboardActor(nameOption: Option[String]) : LeaderboardPostResponse = {
    val memberToScore = new ConcurrentHashMap[String,Option[Score]]
    val scoreToMember = new ConcurrentSkipListMap[Score,String]
    val leaderboard = new ConsecutiveLeaderboard(memberToScore, scoreToMember)
    val leaderboardActor = new LeaderboardActor(leaderboard)

    val uuid = UUID.randomUUID()
    uuidToLeaderboard.put(uuid,leaderboard) match {
      case None =>
        LeaderboardPostResponse(nameOption, uuid.toString)
      case Some(item) =>
        throw new InternalError()
    }
  }

  def synchronizedConcurrentLeaderboard(nameOption: Option[String]) : LeaderboardPostResponse = {
    val memberToScore = new ConcurrentHashMap[String, Option[Score]]
    val scoreToMember = new ConcurrentSkipListMap[Score, String]
    val leaderboard = new SynchronizedConcurrentLeaderboard(memberToScore, scoreToMember)
    val uuid = UUID.randomUUID()
    uuidToLeaderboard.put(uuid,leaderboard) match {
      case None =>
        LeaderboardPostResponse(nameOption, uuid.toString)
      case Some(item) =>
        throw new InternalError()
    }
  }

  def synchronizedLeaderboard(nameOption: Option[String]) : LeaderboardPostResponse = {
    val memberToScore = new util.HashMap[String, Option[Score]]
    val scoreToMember = new util.TreeMap[Score, String]
    val leaderboard = new SynchronizedLeaderboard(memberToScore, scoreToMember)
    val uuid = UUID.randomUUID()
    uuidToLeaderboard.put(uuid,leaderboard) match {
      case None =>
        LeaderboardPostResponse(nameOption, uuid.toString)
      case Some(item) =>
        throw new InternalError()
    }
  }

}
