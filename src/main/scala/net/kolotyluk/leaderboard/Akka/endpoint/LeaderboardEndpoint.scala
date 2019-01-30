package net.kolotyluk.leaderboard.Akka.endpoint

import java.util
import java.util.concurrent.{ConcurrentHashMap, ConcurrentSkipListMap}
import java.util.{NoSuchElementException, UUID}

import akka.event.Logging
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpRequest, HttpResponse}
import akka.http.scaladsl.server.{Directives, ExceptionHandler, MalformedRequestContentRejection, RejectionHandler, RequestEntityExpectedRejection, Route, UnsupportedRequestContentTypeRejection}
import io.swagger.annotations._
import javax.ws.rs.Path
import net.kolotyluk.leaderboard.Akka.{LeaderboardActor, endpoint}
import net.kolotyluk.leaderboard.scorekeeping.{ConcurrentLeaderboard, ConsecutiveLeaderboard, Leaderboard, LeaderboardIdentifier, MemberIdentifier, Score, SynchronizedConcurrentLeaderboard, SynchronizedLeaderboard}
import net.kolotyluk.scala.extras.{Internalized, Logging}
import spray.json.DefaultJsonProtocol

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.language.implicitConversions
import scala.util.Failure

//case class LeaderboardRejection(response: HttpResponse) extends scala.AnyRef with Rejection {
//}

//final case class UnknownKindRejection()
//  extends LeaderboardRejection(null) {
//  def this(kind: String) {
//    this(HttpResponse(BadRequest, entity = s"leaderboard.kind=$kind unknown! Specify one of: ConcurrentLeaderboard, LeaderboardActor, SynchronizedConcurrentLeaderboard, SynchronizedLeaderboard"))
//  }
//}

final case class LeaderboardGetResponse(name: String, state: String, members: Long)

final case class LeaderboardPostRequest(name: Option[String], kind: String)

final case class LeaderboardPostResponse(name: Option[String], id: String)

final case class LeaderboardStatusResponse(id: String, size: Int)

final case class LeaderboardStatusResponses(seq: Seq[LeaderboardStatusResponse])

trait LeaderboardJsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val requestFormat = jsonFormat2(LeaderboardPostRequest)
  implicit val responseFormat = jsonFormat2(LeaderboardPostResponse)
  implicit val leaderboardStatusResponseFormat = jsonFormat2(LeaderboardStatusResponse)
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
  class InconsistentNameException(parameterName: String, payloadName: String)
    extends EndpointException(HttpResponse(BadRequest, entity = s"ambiguous request: leaderboard parameter name=$parameterName and payload name=$payloadName do not match")) {
  }

  class NameNotFoundException(id: String)
    extends EndpointException(HttpResponse(NotFound, entity = s"leaderboard id=$id does not exist")) {
  }

  class UnknownKindException(name: String, kind: String)
    extends EndpointException(HttpResponse(BadRequest,
      entity = s"In {'name' : '$name', 'kind' : '$kind'}, $kind is unknown! Specify one of: ${Kind.values.mkString(", ")}")) {
  }

  class UnknownLeaderboardException(id: String)
  extends EndpointException(HttpResponse(BadRequest,
    entity = s"leaderboard identifier=$id is unknown")) {
  }

  object Kind extends Enumeration {
    protected case class Val(constructor: LeaderboardIdentifier => Leaderboard) extends super.Val {
      def create(nameOption: Option[String]): LeaderboardPostResponse = {
        val leaderboardIdentifier = Internalized[UUID](UUID.randomUUID)
        val leaderboardUrlIdentifier = internalIdentifierToUrlId(leaderboardIdentifier)
        val leaderboard = constructor(leaderboardIdentifier)
        identifierToLeaderboard.put(leaderboardIdentifier,leaderboard) match {
          case None =>
              LeaderboardPostResponse(nameOption, leaderboardUrlIdentifier)
          case Some(item) =>
            // TODO There should not be an existing leaderboard with this ID
            throw new InternalError()
        }
        LeaderboardPostResponse(nameOption, leaderboardUrlIdentifier)
      }
    }
    implicit def valueToKindVal(x: Value): Val = x.asInstanceOf[Val]

    val ConcurrentLeaderboard = Val(
      leaderboardIdentifier => {
        val memberToScore = new ConcurrentHashMap[MemberIdentifier,Option[Score]]
        val scoreToMember = new ConcurrentSkipListMap[Score,MemberIdentifier]
        new ConcurrentLeaderboard(leaderboardIdentifier, memberToScore, scoreToMember)
      }
    )

    val LeaderboardActor = Val(
      leaderboardIdentifier => {
        val memberToScore = new ConcurrentHashMap[MemberIdentifier,Option[Score]]
        val scoreToMember = new ConcurrentSkipListMap[Score,MemberIdentifier]
        val leaderboard = new ConsecutiveLeaderboard(leaderboardIdentifier, memberToScore, scoreToMember)
        val leaderboardActor = new LeaderboardActor(leaderboardIdentifier, leaderboard)
        leaderboard
      }
    )

    val SynchronizedConcurrentLeaderboard = Val(
      leaderboardIdentifier => {
        val memberToScore = new ConcurrentHashMap[MemberIdentifier, Option[Score]]
        val scoreToMember = new ConcurrentSkipListMap[Score, MemberIdentifier]
        new SynchronizedConcurrentLeaderboard(leaderboardIdentifier, memberToScore, scoreToMember)
      }
    )

    val SynchronizedLeaderboard = Val(
      leaderboardIdentifier => {
        val memberToScore = new util.HashMap[MemberIdentifier, Option[Score]]
        val scoreToMember = new util.TreeMap[Score, MemberIdentifier]
        new SynchronizedLeaderboard(leaderboardIdentifier, memberToScore, scoreToMember)
      }
    )
  }

  def routeExceptionHandler: ExceptionHandler =
    ExceptionHandler {
      case cause: DuplicateIDException =>
        logger.error(cause)
        complete(cause.response)
      case cause: EndpointException =>
        logger.warn(cause)
        complete(cause.response)
    }

  def routes: Route =
    logRequest("endpoints", Logging.DebugLevel) {
      handleExceptions(routeExceptionHandler) {
        pathPrefix("leaderboard" ) {
          leaderboardGet() ~
            parameter('name) { name =>
              pathEnd {
                leaderboardGet(name) ~
                  leaderboardPost(Some(name))
              }
            } ~
            //leaderboardGet() ~
            pathEnd {
              leaderboardPost(None) //~
              //        complete {
              //          //HttpEntity(ContentTypes.`text/plain(UTF-8)`, s"Leaderboard query expected")
              //          HttpResponse(BadRequest, entity = "****query missing****")
              //        }
            }
        }
      }
    }

  def leaderboardGet(): Route = {
    get {
      logger.debug(s"**************************** GET")
      path(Segment / Segment ~ PathEnd) {(leaderboardId, memberId) =>
        complete(getLeaderboardStatus(leaderboardId, memberId))
      } ~
      path(Segment) { leaderboardId =>
        logger.debug(s"**************************** leaderboardId=$leaderboardId")
        getLeaderboardStatus(leaderboardId) match {
          case Left(response) =>
            complete(response)
          case Right(response) =>
            onComplete(response) {
              case scala.util.Success(value) => complete(value)
              case Failure(cause) => complete("")
            }
        }
      } ~
      pathEnd {
        complete("")
//        val result = getLeaderboardList
//        validate(result.isInstanceOf[String], s"") {
//          complete("")
//        } ~ {
//          onComplete(result.asInstanceOf[Future[String]]) {
//            case scala.util.Success(value) => complete(value)
//            case Failure(cause) => complete("")
//          }
//        }
//        else
//          onSuccess(result) {
//            case Success(value) => complete(value)
//            case Failure(cause) => complete("boom")
//          }
      }
    }
  }

//  def getLeaderboardList: Response[String] = {
//    val t = leaderboardIdentifierToLeaderboard.toTraversable
//    //t.map[(LeaderboardIdentifier,Leaderboard),String]((a,b) => {""})
//    // case (key, value)
//    val c = t.map{case (identifier, leaderboard) =>
//      val id = identifier.getValue[String]
//      leaderboard.isInstanceOf[LeaderboardAsync] match {
//        case false =>
//          type Response[A] = A
//        case true =>
//          type Response[A] = Future[A]
//      }
//
//      LeaderboardStatusResponse(identifier.getValue[String], leaderboard.getCount)
//    }
//    ""
//  }

  def getLeaderboardStatus(leaderboardId: String): Either[LeaderboardStatusResponse,Future[LeaderboardStatusResponse]] = {
    identifierToLeaderboard.get(endpoint.urlIdToInternalIdentifier(leaderboardId)) match {
      case None =>
        throw new UnknownLeaderboardException(leaderboardId)
      case Some(leaderboard) =>
        val count = leaderboard.getCount
        if (count.isInstanceOf[Int]) {
          Left(LeaderboardStatusResponse(leaderboardId, count.asInstanceOf[Int]))
        } else {
          Right(count.asInstanceOf[Future[Int]].map{ futureCount =>
            LeaderboardStatusResponse(leaderboardId, futureCount)
          })
        }
    }
  }

  def getLeaderboardStatus(leaderboardId: String, memberId: String) = {
    ""
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
    * @param nameParameter
    * @return
    */
  @Path("?name={name}")
  @ApiOperation(value = "Return info on named leaderboard", notes = "", nickname = "leaderboardPost", httpMethod = "POST")
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "Return leaderboard created confirmation", response = classOf[String]),
    new ApiResponse(code = 405, message = "Return leaderboard already exists", response = classOf[String])
  ))
  def leaderboardPost(nameParameter: Option[String]): Route =
    post {
      //logRequest("leaderboard", Logging.DebugLevel) {
      //  handleExceptions(routeExceptionHandler) {
          handleRejections(postRejectionHandler) {
            entity(as[LeaderboardPostRequest]) { leaderboard =>
              // unix shell: curl -H "Content-Type: application/json" -d '{"name":"foo","kind":"ConcurrentLeaderboard"}' -X POST http://localhost:8080/leaderboard
              // PowerShell: Invoke-WebRequest http://localhost:8080/leaderboard -Method Post -ContentType "application/json" -Body '{"name":"foo","kind":"ConcurrentLeaderboard"}'
              try {
                leaderboard.name match {
                  case None =>
                    complete(leaderboardCreate(nameParameter, Some(leaderboard.kind)))
                  case Some(payloadName) =>
                    nameParameter match {
                      case None =>
                        complete(leaderboardCreate(leaderboard.name, Some(leaderboard.kind)))
                      case Some(parameterName) =>
                        if (payloadName == parameterName)
                          complete(leaderboardCreate(leaderboard.name, Some(leaderboard.kind)))
                        else
                          throw new InconsistentNameException(parameterName, payloadName)
                    }
                }
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
                complete(leaderboardCreate(nameParameter, None)) // default leaderboard
              else
                complete(HttpResponse(BadRequest, entity = "****  Something Wrong  ****")) // TODO this better
            }
          }
      //  }
      //}
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
        if (nameToLeaderboardIdentifier.contains(name)) {
          throw new DuplicateNameException(name)
        } else create()
    }
  }

}
