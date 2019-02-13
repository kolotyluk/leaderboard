package net.kolotyluk.leaderboard.Akka.endpoint.leaderboard

import java.util.NoSuchElementException

import akka.event.Logging
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpResponse}
import akka.http.scaladsl.server._
import io.swagger.annotations._
import javax.ws.rs.Path
import net.kolotyluk.leaderboard.Akka.endpoint
import net.kolotyluk.leaderboard.Akka.endpoint.leaderboard.failure.{UnknownImplementationException, UnknownLeaderboardIdentifierException}
import net.kolotyluk.leaderboard.Akka.endpoint.{EndpointError, EndpointException, EndpointException2}
import net.kolotyluk.leaderboard.scorekeeping.{ConcurrentLeaderboard, Increment, LeaderboardIdentifier, MemberIdentifier, Replace, Score, UpdateMode}
import net.kolotyluk.scala.extras.Logging

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}

@Api(value = "/leaderboard", produces = "text/plain(UTF-8)")
@Path("/leaderboard")
class Endpoint extends Directives with JsonSupport with PrettyJasonSupport with Logging {

  def exceptionHandler: ExceptionHandler =
    ExceptionHandler {
      case cause: EndpointException =>
        logger.warn(cause)
        complete(cause.response)
      case cause: EndpointError =>
        logger.error(cause.errorPayload.systemLogMessage, cause.getCause)
        complete(cause.statusCode, cause.errorPayload)
      case cause: EndpointException2 =>
        logger.warn(cause.errorPayload.systemLogMessage)
        complete(cause.statusCode, cause.errorPayload)
    }

  def routes: Route =
    logRequest("leaderboard", Logging.DebugLevel) {
      handleExceptions(exceptionHandler) {
        pathPrefix("leaderboard" ) {
          leaderboardGet() ~    // get resource details
          leaderboardPost() ~   // create new resource
          leaderboardPut() ~    // replace current score
          leaderboardPatch() ~  // increment current score
          complete{
            HttpResponse(BadRequest, entity = "Bad /leaderboard request")
          }
        }
      }
    }

  /** =GET leaderboard=
    *
    * ==examples==
    * {{{
    * unix shell: curl http://localhost:8080/leaderboard/fiqkXE39T_WcUcCPtGcoaQ
    * PowerShell: Invoke-WebRequest -Method Get http://localhost:8080/leaderboard/fiqkXE39T_WcUcCPtGcoaQ
    * }}}
    * @return
    */
    def leaderboardGet1(): Route = {
      get {
        pathEnd {
          // unix shell: curl http://localhost:8080/leaderboard?name=foo
          // PowerShell: Invoke-WebRequest -Method Get http://localhost:8080/leaderboard?name=foo
          parameter('name) { name =>
            nameToLeaderboardIdentifier.get(name) match {
              case None =>
                throw new UnknownLeaderboardNameException(name)
              case Some(leaderboardIdentifier) =>
                onComplete(getLeaderboardStatus(leaderboardIdentifier)) {
                  case Success(value) => complete(value)
                  case Failure(cause: EndpointException) => complete(cause.response)
                  case Failure(cause) => complete("")
                }
            }
          } ~
          // unix shell: curl http://localhost:8080/leaderboard
          // PowerShell: Invoke-WebRequest -Method Get http://localhost:8080/leaderboard
          onComplete(getLeaderboards) {
            case scala.util.Success(value) => complete(value)
            case Failure(cause: EndpointException) => complete(cause.response)
            case Failure(cause) => complete("")
          }
        } ~
        pathPrefix(base64identifier) { leaderboardIdentifier =>
          pathEnd {
            // unix shell: curl http://localhost:8080/leaderboard/fiqkXE39T_WcUcCPtGcoaQ
            // PowerShell: Invoke-WebRequest -Method Get http://localhost:8080/leaderboard/fiqkXE39T_WcUcCPtGcoaQ
            onComplete(getLeaderboardStatus(leaderboardIdentifier)) {
              case Success(value) => complete(value)
              case Failure(cause: EndpointException) => complete(cause.response)
              case Failure(cause) => complete("") // TODO
            }
          } ~
          pathPrefix(base64identifier) { memberIdentifier =>
            pathEnd {
              // unix shell: curl http://localhost:8080/leaderboard/fiqkXE39T_WcUcCPtGcoaQ/keAoZQECSwm0h7v6yw_3WQ
              // PowerShell: Invoke-WebRequest -Method Get http://localhost:8080/leaderboard/fiqkXE39T_WcUcCPtGcoaQ/keAoZQECSwm0h7v6yw_3WQ
              complete(getLeaderboardStatus(leaderboardIdentifier, memberIdentifier))
            }
          }
        } ~
        complete {
          HttpResponse(BadRequest, entity = "Bad GET /leaderboard request")
        }
      }
    }

    def leaderboardGet(): Route = {
      get {
        pathPrefix(base64identifier) { leaderboardIdentifier =>
          pathPrefix(base64identifier) { memberIdentifier =>
            pathEnd {
              // unix shell: curl http://localhost:8080/leaderboard/fiqkXE39T_WcUcCPtGcoaQ/keAoZQECSwm0h7v6yw_3WQ
              // PowerShell: Invoke-WebRequest -Method Get http://localhost:8080/leaderboard/fiqkXE39T_WcUcCPtGcoaQ/keAoZQECSwm0h7v6yw_3WQ
              complete(getLeaderboardStatus(leaderboardIdentifier, memberIdentifier))
            }
          } ~
          pathEnd {
            // unix shell: curl http://localhost:8080/leaderboard/fiqkXE39T_WcUcCPtGcoaQ
            // PowerShell: Invoke-WebRequest -Method Get http://localhost:8080/leaderboard/fiqkXE39T_WcUcCPtGcoaQ
            onComplete(getLeaderboardStatus(leaderboardIdentifier)) {
              case Success(value) =>
                logger.debug(s"%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%")
                complete(value)
              case Failure(cause: EndpointException) =>
                logger.error("//////////////////////////////////////////////////")
                complete(cause.response)
              case Failure(cause) =>
                logger.error("<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<")
                complete("") // TODO
            }
          }
        } ~
        pathEnd {
          // unix shell: curl http://localhost:8080/leaderboard?name=foo
          // PowerShell: Invoke-WebRequest -Method Get http://localhost:8080/leaderboard/name=foo
          parameter('name) { name =>
            nameToLeaderboardIdentifier.get(name) match {
              case None =>
                throw new UnknownLeaderboardNameException(name)
              case Some(leaderboardIdentifier) =>
                onComplete(getLeaderboardStatus(leaderboardIdentifier)) {
                  case Success(value) => complete(value)
                  case Failure(cause: EndpointException) => complete(cause.response)
                  case Failure(cause) => complete("")
                }
            }
          } ~
            // unix shell: curl http://localhost:8080/leaderboard
            // PowerShell: Invoke-WebRequest -Method Get http://localhost:8080/leaderboard
            onComplete(getLeaderboards) {
              case scala.util.Success(value) => complete(value)
              case Failure(cause: EndpointException) => complete(cause.response)
              case Failure(cause) => complete("")
            }
        } ~
        complete {
          HttpResponse(BadRequest, entity = "Bad GET /leaderboard request")
        }
      }
    }

  /** =PUT leaderboard=
    *
    * ==curl examples==
    * {{{
    * unix shell: curl http://localhost:8080/leaderboard/fiqkXE39T_WcUcCPtGcoaQ
    * PowerShell: Invoke-WebRequest -Method Get http://localhost:8080/leaderboard/fiqkXE39T_WcUcCPtGcoaQ
    * }}}
    * @return
    */
  def leaderboardPut(): Route = {
    put {
      scoreUpdate(Replace)
    }
  }

  def leaderboardPatch(): Route = {
    put {
      scoreUpdate(Increment)
    }
  }

  /** =Score Update Route=
    * Philosophical question: Do we really want to use two different HTTP Methods to update member scores on a
    * leaderboard, or should we just overload PATCH?
    *
    * @param updateMode Increment or Replace
    * @return
    */
  def scoreUpdate(updateMode: UpdateMode): Route = {
    path(base64identifier / base64identifier ~ PathEnd) {(leaderboardIdentifier, memberIdentifier) =>
      parameter('score) { score =>
        complete(updateScore(updateMode, leaderboardIdentifier, memberIdentifier, BigInt(score)))
      } ~
      entity(as[UpdateScoreRequest]) { request =>
        complete(updateScore(updateMode, leaderboardIdentifier, memberIdentifier, BigInt(request.score)))
      }
    } ~
      path(base64identifier ~ PathEnd) { leaderboardId =>
        onComplete(getLeaderboardStatus(leaderboardId)) {
          case Success(value) => complete(value)
          case Failure(cause: EndpointException) => complete(cause.response)
          case Failure(cause) => complete("") // TODO
        }
      } ~
      pathEnd {
        parameter('name) { name =>
          nameToLeaderboardIdentifier.get(name) match {
            case None =>
              throw new UnknownLeaderboardNameException(name)
            case Some(leaderboardIdentifier) =>
              onComplete(getLeaderboardStatus(leaderboardIdentifier)) {
                case Success(value) => complete(value)
                case Failure(cause: EndpointException) => complete(cause.response)
                case Failure(cause) => complete("") // TODO
              }
          }
        } ~
          onComplete(getLeaderboards) {
            case Success(value) => complete(value)
            case Failure(cause: EndpointException) => complete(cause.response)
            case Failure(cause) => complete("") // TODO
          }
      }
  }

  def updateScore(updateMode: UpdateMode, leaderboardIdentifier: LeaderboardIdentifier, memberIdentifier: MemberIdentifier, score: BigInt) : Future[MemberStatusResponse] = {
    val leaderboardUrlId = endpoint.internalIdentifierToUrlId(leaderboardIdentifier)
    val memberUrlId = endpoint.internalIdentifierToUrlId(memberIdentifier)
    identifierToLeaderboard.get(leaderboardIdentifier) match {
      case None =>
        throw new UnknownLeaderboardIdentifierException(leaderboardIdentifier)
      case Some(leaderboard) =>
        val newScore = leaderboard.update(updateMode, memberIdentifier, score)
        if (newScore.isInstanceOf[Score]) {
          Future.successful(MemberStatusResponse(Some(leaderboardUrlId), Some(memberUrlId), newScore.asInstanceOf[Score].toString))
        } else {
          newScore.asInstanceOf[Future[Score]].map{ futureScore =>
            MemberStatusResponse(Some(leaderboardUrlId), Some(memberUrlId), futureScore.toString)
          }
        }
    }
  }

  def getLeaderboards: Future[LeaderboardStatusResponses] = {
    val statusResponses = identifierToLeaderboard.map{case (identifier, leaderboard) =>
      val leaderboardId = endpoint.internalIdentifierToUrlId(identifier)
      val count = leaderboard.getCount
      if (count.isInstanceOf[Int]) {
        Future.successful(LeaderboardStatusResponse(leaderboardId, count.asInstanceOf[Int]))
      } else {
        count.asInstanceOf[Future[Int]].map{ futureCount =>
          LeaderboardStatusResponse(leaderboardId, futureCount)
        }
      }
    }
    Future.sequence(statusResponses).map{ leaderboardStatusResponse =>
      LeaderboardStatusResponses(leaderboardStatusResponse.toSeq)
    }
  }

  def getLeaderboardStatus(leaderboardUrlId: String): Future[LeaderboardStatusResponse] = {
    getLeaderboardStatus(endpoint.urlIdToInternalIdentifier(leaderboardUrlId))
  }

  def getLeaderboardStatus(leaderboardIdentifier: LeaderboardIdentifier) = {
    val leaderboardUrlId = endpoint.internalIdentifierToUrlId(leaderboardIdentifier)
    logger.debug(s"getLeaderboardStatus: $leaderboardUrlId : ${leaderboardIdentifier.getValue}")
    identifierToLeaderboard.get(leaderboardIdentifier) match {
      case None =>
        throw new UnknownLeaderboardIdentifierException(leaderboardIdentifier)
      case Some(leaderboard) =>
        leaderboard.getCount match {
          case future: Future[Int] =>
            future.map{ count =>
              LeaderboardStatusResponse(leaderboardUrlId, count)
            }
          case count: Int =>
            Future.successful(LeaderboardStatusResponse(leaderboardUrlId, count))
        }
    }
  }

  def getLeaderboardStatus(leaderboardIdentifier: LeaderboardIdentifier, memberIdentifier: MemberIdentifier): Future[MemberStatusResponse] = {
    logger.debug(s"getLeaderboardStatus: leaderboardIdentifier=$leaderboardIdentifier memberIdentifier=$memberIdentifier")
    identifierToLeaderboard.get(leaderboardIdentifier) match {
      case None =>
        throw new UnknownLeaderboardIdentifierException(leaderboardIdentifier)
      case Some(leaderboard) =>
        val result = leaderboard.getScore(memberIdentifier)
        if (result.isInstanceOf[Option[_]]) {
          result.asInstanceOf[Option[scala.BigInt]] match {
            case None =>
              Future.successful(MemberStatusResponse(None, None, ""))
            case Some(score) =>
              Future.successful(MemberStatusResponse(None, None, score.toString))
          }
        } else {
          result.asInstanceOf[Future[Option[scala.BigInt]]].map {futureResult =>
            futureResult match {
              case None =>
                MemberStatusResponse(None, None, "")
              case Some(score) =>
                MemberStatusResponse(None, None, score.toString)
            }
          }
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
    * PowerShell: Invoke-WebRequest http://localhost:8080/leaderboard -Method Post -ContentType "application/json" -Body '{"name":"foo","kind":"LeaderboardActor"}'
    * }}}
    * @param nameParameter
    * @return
    */
  @Path("?name={name}")
  @ApiOperation(value = "Return info on named leaderboard", notes = "", nickname = "leaderboardPost", httpMethod = "POST")
  @ApiResponses(Array(
    new ApiResponse(code = 201, message = "Return leaderboard created confirmation", response = classOf[String]),
    new ApiResponse(code = 405, message = "Return leaderboard already exists", response = classOf[String])
  ))
  def leaderboardPost(): Route =
    post {
      //logRequest("leaderboard", Logging.DebugLevel) {
      //  handleExceptions(routeExceptionHandler) {
      handleRejections(postRejectionHandler) {
        pathEnd {
          parameter('name.?) { nameParameter =>
            entity(as[LeaderboardPostRequest]) { leaderboard =>
              // unix shell: curl -H "Content-Type: application/json" -d '{"name":"foo","kind":"ConcurrentLeaderboard"}' -X POST http://localhost:8080/leaderboard
              // PowerShell: Invoke-WebRequest http://localhost:8080/leaderboard -Method Post -ContentType "application/json" -Body '{"name":"foo","kind":"ConcurrentLeaderboard"}'
              complete(Created, leaderboardCreate(nameParameter, Some(leaderboard)))
            } ~
            complete(Created, leaderboardCreate(nameParameter, None)) // default leaderboard
            //          entity(as[HttpRequest]) { httpRequest =>
            //            // TODO do we really need this?
            //            if (httpRequest.entity.getContentLengthOption().getAsLong == 0)
            //            // unix shell: curl -d "" http://localhost:8080/leaderboard?name=foo
            //            // PowerShell: Invoke-WebRequest -Method Post http://localhost:8080/leaderboard?name=foo
            //              complete(leaderboardCreate(nameParameter, None)) // default leaderboard
            //            else
            //              complete(HttpResponse(BadRequest, entity = "****  Something Wrong  ****")) // TODO this better
            //          }
          }
        }
      }
    }

  def leaderboardCreate(parameterNameOption: Option[String], payload: Option[LeaderboardPostRequest]): LeaderboardPostResponse = {

    // Node: this code is complicated by the fact we are trying to support named leaderboards, and hand handle names in
    // both the URL parameter and the payload, which might be in conflict. It might be better to reject the URL parameter
    // if there is a payload. It might be better to avoid named leaderboards altogether. EK

    def create(leaderboardNameOption: Option[String], leaderboardPostRequest: LeaderboardPostRequest): LeaderboardPostResponse = {
      try {
        Implementation.withName(leaderboardPostRequest.implementationName).create(leaderboardNameOption)
      } catch {
        case _: NoSuchElementException =>
          throw new UnknownImplementationException(leaderboardPostRequest, leaderboardPostRequest.implementationName)
      }
    }

    def verifyAndCreate(leaderboardName: String,  leaderboardPostRequest: LeaderboardPostRequest): LeaderboardPostResponse = {
      if (nameToLeaderboardIdentifier.contains(leaderboardName))
        throw new DuplicateNameException(leaderboardName)
      create(Some(leaderboardName), leaderboardPostRequest)
    }

    payload match {
      case None =>
        // There is no payload, so no payload.leaderboardName to worry about
        Implementation.LeaderboardActor.create(parameterNameOption) // Default implementation

      case Some(leaderboardPostRequest) =>
        parameterNameOption match {
          case None =>
            create(leaderboardPostRequest.leaderboardName, leaderboardPostRequest)
          case Some(parameterName) =>
            leaderboardPostRequest.leaderboardName match {
              case None =>
                verifyAndCreate(parameterName, leaderboardPostRequest)
              case Some(payloadName) =>
                if (parameterName != payloadName)
                  throw new InconsistentNameException(parameterName, payloadName)
                verifyAndCreate(payloadName, leaderboardPostRequest)
            }
        }
    }
  }
}
