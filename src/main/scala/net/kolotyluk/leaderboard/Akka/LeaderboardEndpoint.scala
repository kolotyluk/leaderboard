package net.kolotyluk.leaderboard.Akka

import java.util
import java.util.{NoSuchElementException, UUID}
import java.util.concurrent.{ConcurrentHashMap, ConcurrentSkipListMap}

import akka.event.Logging
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpRequest, HttpResponse}
import akka.http.scaladsl.server.{Directives, ExceptionHandler, MalformedRequestContentRejection, RejectionHandler, RequestEntityExpectedRejection, Route, UnsupportedRequestContentTypeRejection}
import io.swagger.annotations._
import javax.ws.rs.Path
import net.kolotyluk.leaderboard.Akka.endpoint.EndpointException
import net.kolotyluk.leaderboard.scorekeeping.{ConcurrentLeaderboard, ConsecutiveLeaderboard, Leaderboard, Score, SynchronizedConcurrentLeaderboard, SynchronizedLeaderboard}
import net.kolotyluk.scala.extras.Logging
import spray.json.DefaultJsonProtocol

import scala.language.implicitConversions

//case class LeaderboardRejection(response: HttpResponse) extends scala.AnyRef with Rejection {
//}

//final case class UnknownKindRejection()
//  extends LeaderboardRejection(null) {
//  def this(kind: String) {
//    this(HttpResponse(BadRequest, entity = s"leaderboard.kind=$kind unknown! Specify one of: ConcurrentLeaderboard, LeaderboardActor, SynchronizedConcurrentLeaderboard, SynchronizedLeaderboard"))
//  }
//}

final case class LeaderboardGetResponse(name: String, state: String, members: Long)

final case class LeaderboardPostRequest(name: String, kind: String)

final case class LeaderboardPostResponse(name: Option[String], id: String)

trait LeaderboardJsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val requestFormat = jsonFormat2(LeaderboardPostRequest)
  implicit val responseFormat = jsonFormat2(LeaderboardPostResponse)
}

@Api(value = "/leaderboard", produces = "text/plain(UTF-8)")
@Path("/leaderboard")
class LeaderboardEndpoint extends Directives with LeaderboardJsonSupport with Logging {

  class DuplicateIDException(id: UUID)
    extends EndpointException(HttpResponse(InternalServerError, entity = s"leaderboard id=$id already exists")) {
  }

  class DuplicateNameException(name: String)
    extends EndpointException(HttpResponse(BadRequest, entity = s"leaderboard?name=$name already exists")) {
  }

  class IdNotFoundException(name: String)
    extends EndpointException(HttpResponse(NotFound, entity = s"leaderboard name=$name does not exist")) {
  }

  class NameNotFoundException(id: String)
    extends EndpointException(HttpResponse(NotFound, entity = s"leaderboard id=$id does not exist")) {
  }
  class UnknownKindException(name: String, kind: String)
    extends EndpointException(HttpResponse(BadRequest,
      entity = s"In {'name' : '$name', 'kind' : '$kind'}, $kind is unknown! Specify one of: ${Kind.values.mkString(", ")}")) {
  }

  object Kind extends Enumeration {
    protected case class Val(constructor: UUID => Leaderboard) extends super.Val {
      def create(nameOption: Option[String]): LeaderboardPostResponse = {
        val uuid = UUID.randomUUID
        val leaderboard = constructor(uuid)
        uuidToLeaderboard.put(uuid,leaderboard) match {
          case None =>
            LeaderboardPostResponse(nameOption, uuid.toString)
          case Some(item) =>
            // TODO There should not be an existing leaderboard with this ID
            throw new InternalError()
        }
        LeaderboardPostResponse(nameOption, uuid.toString)
      }
    }
    implicit def valueToKindVal(x: Value): Val = x.asInstanceOf[Val]

    val ConcurrentLeaderboard = Val(
      uuid => {
        val memberToScore = new ConcurrentHashMap[String,Option[Score]]
        val scoreToMember = new ConcurrentSkipListMap[Score,String]
        new ConcurrentLeaderboard(memberToScore, scoreToMember)
      }
    )

    val LeaderboardActor = Val(
      uuid => {
        val memberToScore = new ConcurrentHashMap[String,Option[Score]]
        val scoreToMember = new ConcurrentSkipListMap[Score,String]
        val leaderboard = new ConsecutiveLeaderboard(memberToScore, scoreToMember)
        val leaderboardActor = new LeaderboardActor(leaderboard)
        leaderboard
      }
    )

    val SynchronizedConcurrentLeaderboard = Val(
      uuid => {
        val memberToScore = new ConcurrentHashMap[String, Option[Score]]
        val scoreToMember = new ConcurrentSkipListMap[Score, String]
        new SynchronizedConcurrentLeaderboard(memberToScore, scoreToMember)
      }
    )

    val SynchronizedLeaderboard = Val(
      name => {
        val memberToScore = new util.HashMap[String, Option[Score]]
        val scoreToMember = new util.TreeMap[Score, String]
        new SynchronizedLeaderboard(memberToScore, scoreToMember)
      }
    )
  }

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

  def postExceptionHandler: ExceptionHandler =
    ExceptionHandler {
      case cause: DuplicateIDException =>
        logger.error(cause)
        complete(cause.response)
      case cause: EndpointException =>
        logger.warn(cause)
        complete(cause.response)
    }

  val postRejectionHandler = RejectionHandler.newBuilder().handle{
      case RequestEntityExpectedRejection =>
        complete(BadRequest, "RequestEntityExpectedRejection") // TODO there seems to be no way to get here?
      case UnsupportedRequestContentTypeRejection(supported) =>
        complete(BadRequest, s"UnsupportedRequestContentTypeRejection, expecting one of: ${supported.mkString(", ")}")
      case MalformedRequestContentRejection(message, cause) =>
        logger.error(cause)
        complete(BadRequest, s"MalformedRequestContentRejection $message")
//      case LeaderboardRejection(response) =>
//        complete(response)
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
    * @param nameOption
    * @return
    */
  @Path("?name={name}")
  @ApiOperation(value = "Return info on named leaderboard", notes = "", nickname = "leaderboardPost", httpMethod = "POST")
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "Return leaderboard created confirmation", response = classOf[String]),
    new ApiResponse(code = 405, message = "Return leaderboard already exists", response = classOf[String])
  ))
  def leaderboardPost(nameOption: Option[String]): Route =
    post {
      logRequest("leaderboard", Logging.DebugLevel) {
        handleExceptions(postExceptionHandler) {
          handleRejections(postRejectionHandler) {
            entity(as[LeaderboardPostRequest]) { leaderboard =>
              // unix shell: curl -H "Content-Type: application/json" -d '{"name":"foo","kind":"ConcurrentLeaderboard"}' -X POST http://localhost:8080/leaderboard
              // PowerShell: Invoke-WebRequest http://localhost:8080/leaderboard -Method Post -ContentType "application/json" -Body '{"name":"foo","kind":"ConcurrentLeaderboard"}'
              try {
                complete(leaderboardCreate(Some(leaderboard.name), Some(leaderboard.kind)))
              } catch {
                case cause: DuplicateIDException =>
                  logger.error(cause)
                  complete(cause.response)
                case cause: EndpointException =>
                  logger.warn(cause)
                  complete(cause.response)
                case cause: Throwable =>
                  logger.error(cause)
                  complete(HttpResponse(InternalServerError, entity = s"Exception thrown from LeaderboardPost: ${cause.getMessage}"))
              }
            } ~
            entity(as[HttpRequest]) { httpRequest =>
              if (httpRequest.entity.getContentLengthOption().getAsLong == 0)
                // unix shell: curl -d "" http://localhost:8080/leaderboard?name=foo
                // PowerShell: Invoke-WebRequest -Method Post http://localhost:8080/leaderboard?name=foo
                complete(leaderboardCreate(nameOption, None)) // default leaderboard
              else
                complete(HttpResponse(BadRequest, entity = "****  Something Wrong  ****")) // TODO this better
            }
          }
        }
      }
    }

  def leaderboardCreate(nameOption: Option[String], kindOption: Option[String]): LeaderboardPostResponse = {
    def create(): LeaderboardPostResponse = {
      kindOption match {
        case None =>
          Kind.LeaderboardActor.create(nameOption)
        case Some(kind) =>
          try {
            Kind.withName(kind).create(nameOption)
          } catch {
            case cause: NoSuchElementException =>
              throw new UnknownKindException(nameOption.getOrElse(""), kind)
          }
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

}
