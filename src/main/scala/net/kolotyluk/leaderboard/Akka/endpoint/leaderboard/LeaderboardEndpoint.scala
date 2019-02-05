package net.kolotyluk.leaderboard.Akka.endpoint.leaderboard

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
import net.kolotyluk.leaderboard.Akka.endpoint.EndpointException
import net.kolotyluk.leaderboard.Akka.{LeaderboardActor, endpoint}
import net.kolotyluk.leaderboard.scorekeeping.{ConcurrentLeaderboard, ConsecutiveLeaderboard, Leaderboard, LeaderboardIdentifier, MemberIdentifier, Replace, Score, SynchronizedConcurrentLeaderboard, SynchronizedLeaderboard}
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

final case class LeaderboardPutScoreRequest(leaderboardId: Option[String], memberId: Option[String], score: String)
final case class LeaderboardPutScoreResponse(leaderboardId: Option[String], memberId: Option[String], score: String)

final case class LeaderboardPutScoresRequest(scores: Seq[LeaderboardPutScoreRequest])

final case class LeaderboardPostResponse(name: Option[String], id: String)

final case class LeaderboardStatusResponse(id: String, size: Int)

final case class LeaderboardStatusResponses(leaderboards: Seq[LeaderboardStatusResponse])

trait LeaderboardJsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val requestFormat = jsonFormat2(LeaderboardPostRequest)
  implicit val responseFormat = jsonFormat2(LeaderboardPostResponse)

  implicit val putScoreRequestFormat = jsonFormat3(LeaderboardPutScoreRequest)
  implicit val putScoreResponseFormat = jsonFormat3(LeaderboardPutScoreResponse)

  implicit val leaderboardStatusResponseFormat = jsonFormat2(LeaderboardStatusResponse)
  implicit val leaderboardStatusResponsesFormat = jsonFormat1(LeaderboardStatusResponses)
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

  class UnknownLeaderboardException(leaderboardIdentifier: LeaderboardIdentifier)
    extends EndpointException(HttpResponse(BadRequest,
      entity = s"leaderboard uuid=${leaderboardIdentifier.value} urlId=${endpoint.internalIdentifierToUrlId(leaderboardIdentifier)} is unknown")) {
  }

  class UnknownLeaderboardIdentifierException(urlId: String)
    extends EndpointException(HttpResponse(BadRequest,
      entity = s"leaderboard urlId=$urlId is unknown")) {
  }

  class UnknownLeaderboardNameException(name: String)
    extends EndpointException(HttpResponse(BadRequest,
      entity = s"leaderboard name=$name unknown")) {
  }

  object Kind extends Enumeration {
    protected case class Val(constructor: LeaderboardIdentifier => Leaderboard) extends super.Val {
      def create(nameOption: Option[String]): LeaderboardPostResponse = {
        val leaderboardIdentifier = Internalized[UUID](UUID.randomUUID)
        val leaderboardUrlIdentifier = endpoint.internalIdentifierToUrlId(leaderboardIdentifier)
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
        val memberToScore = new util.HashMap[MemberIdentifier,Option[Score]]
        val scoreToMember = new util.TreeMap[Score,MemberIdentifier]
        val leaderboard = new ConsecutiveLeaderboard(leaderboardIdentifier, memberToScore, scoreToMember)
        new LeaderboardActor(leaderboardIdentifier, leaderboard)
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
      case cause: EndpointException =>
        logger.warn(cause)
        complete(cause.response)
    }

  def routes: Route =
    logRequest("endpoints", Logging.DebugLevel) {
      handleExceptions(routeExceptionHandler) {
        pathPrefix("leaderboard" ) {
          leaderboardGet() ~
          leaderboardPost() //~
          //pathEnd {
            // leaderboardPost(None) //~
            //        complete {
            //          //HttpEntity(ContentTypes.`text/plain(UTF-8)`, s"Leaderboard query expected")
            //          HttpResponse(BadRequest, entity = "****query missing****")
            //        }
          //}
        }
      }
    }

  /** =GET leaderboard=
    *
    * ==curl examples==
    * {{{
    * unix shell: curl http://localhost:8080/leaderboard/fiqkXE39T_WcUcCPtGcoaQ
    * PowerShell: Invoke-WebRequest -Method Get http://localhost:8080/leaderboard/fiqkXE39T_WcUcCPtGcoaQ
    * }}}
    * @return
    */
  def leaderboardGet(): Route = {
    get {
      path(base64identifier / base64identifier ~ PathEnd) {(leaderboardId, memberId) =>
        complete(getLeaderboardStatus(leaderboardId, memberId))
      } ~
      path(base64identifier ~ PathEnd) { leaderboardId =>
        onComplete(getLeaderboardStatus(leaderboardId)) {
          case scala.util.Success(value) => complete(value)
          case Failure(cause: EndpointException) => complete(cause.response)
          case Failure(cause) => complete("")
        }
      } ~
      pathEnd {
        parameter('name) { name =>
          nameToLeaderboardIdentifier.get(name) match {
            case None =>
              throw new UnknownLeaderboardNameException(name)
            case Some(leaderboardIdentifier) =>
              onComplete(getLeaderboardStatus(leaderboardIdentifier)) {
                case scala.util.Success(value) => complete(value)
                case Failure(cause: EndpointException) => complete(cause.response)
                case Failure(cause) => complete("")
              }
          }
        } ~
        onComplete(getLeaderboards) {
          case scala.util.Success(value) => complete(value)
          case Failure(cause: EndpointException) => complete(cause.response)
          case Failure(cause) => complete("")
        }
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
      path(Segment / Segment ~ PathEnd) {(leaderboardId, memberId) =>
        val leaderboardIdentifier = endpoint.urlIdToInternalIdentifier(leaderboardId)
        val memberIdentifier = endpoint.urlIdToInternalIdentifier(memberId)
        parameter('score) { score =>
          complete(putScore(leaderboardIdentifier, memberIdentifier, BigInt(score)))
        } ~
        entity(as[LeaderboardPutScoreRequest]) { request =>
          complete(putScore(leaderboardIdentifier, memberIdentifier, BigInt(request.score)))
        }
      } ~
        path(Segment ~ PathEnd) { leaderboardId =>
          onComplete(getLeaderboardStatus(leaderboardId)) {
            case scala.util.Success(value) => complete(value)
            case Failure(cause: EndpointException) => complete(cause.response)
            case Failure(cause) => complete("")
          }
        } ~
        pathEnd {
          parameter('name) { name =>
            nameToLeaderboardIdentifier.get(name) match {
              case None =>
                throw new UnknownLeaderboardNameException(name)
              case Some(leaderboardIdentifier) =>
                onComplete(getLeaderboardStatus(leaderboardIdentifier)) {
                  case scala.util.Success(value) => complete(value)
                  case Failure(cause: EndpointException) => complete(cause.response)
                  case Failure(cause) => complete("")
                }
            }
          } ~
            onComplete(getLeaderboards) {
              case scala.util.Success(value) => complete(value)
              case Failure(cause: EndpointException) => complete(cause.response)
              case Failure(cause) => complete("")
            }
        }
    }
  }

  def putScore(leaderboardIdentifier: LeaderboardIdentifier, memberIdentifier: MemberIdentifier, score: BigInt) : Future[LeaderboardPutScoreResponse] = {
    val leaderboardUrlId = endpoint.internalIdentifierToUrlId(leaderboardIdentifier)
    val memberUrlId = endpoint.internalIdentifierToUrlId(memberIdentifier)
    identifierToLeaderboard.get(leaderboardIdentifier) match {
      case None =>
        throw new UnknownLeaderboardIdentifierException(leaderboardUrlId)
      case Some(leaderboard) =>
        val newScore = leaderboard.update(Replace, memberIdentifier, score)
        if (newScore.isInstanceOf[Score]) {
          Future.successful(LeaderboardPutScoreResponse(Some(leaderboardUrlId), Some(memberUrlId), newScore.asInstanceOf[Score].toString))
        } else {
          newScore.asInstanceOf[Future[Score]].map{ futureScore =>
            LeaderboardPutScoreResponse(Some(leaderboardUrlId), Some(memberUrlId), futureScore.toString)
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
        throw new UnknownLeaderboardIdentifierException(leaderboardUrlId)
      case Some(leaderboard) =>
        val count = leaderboard.getCount
        logger.debug(s"count = $count")
        if (count.isInstanceOf[Int]) {
          Future.successful(LeaderboardStatusResponse(leaderboardUrlId, count.asInstanceOf[Int]))
        } else {
          count.asInstanceOf[Future[Int]].map{ futureCount =>
            LeaderboardStatusResponse(leaderboardUrlId, futureCount)
          }
        }
    }
  }

  def getLeaderboardStatus(leaderboardIdentifier: LeaderboardIdentifier, memberIdentifier: MemberIdentifier) = {
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
    * PowerShell: Invoke-WebRequest http://localhost:8080/leaderboard -Method Post -ContentType "application/json" -Body '{"name":"foo","kind":"LeaderboardActor"}'
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
  def leaderboardPost(): Route =
    post {
      //logRequest("leaderboard", Logging.DebugLevel) {
      //  handleExceptions(routeExceptionHandler) {
      handleRejections(postRejectionHandler) {
        parameter('name.?) { nameParameter =>
          entity(as[LeaderboardPostRequest]) { leaderboard =>
            // unix shell: curl -H "Content-Type: application/json" -d '{"name":"foo","kind":"ConcurrentLeaderboard"}' -X POST http://localhost:8080/leaderboard
            // PowerShell: Invoke-WebRequest http://localhost:8080/leaderboard -Method Post -ContentType "application/json" -Body '{"name":"foo","kind":"ConcurrentLeaderboard"}'
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

    ///logger.warn(s"\n\n\n\n\n---------------------> nameOption = $nameOption\n\n\n\n\n")

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
