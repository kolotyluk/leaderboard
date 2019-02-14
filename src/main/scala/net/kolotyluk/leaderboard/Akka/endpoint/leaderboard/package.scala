package net.kolotyluk.leaderboard.Akka.endpoint

import java.util.UUID

import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.model.StatusCodes.{BadRequest, NotFound}
import akka.http.scaladsl.server.PathMatcher1
import net.kolotyluk.akka.scala.extras.base64uuid
import net.kolotyluk.leaderboard.InternalIdentifier
import net.kolotyluk.leaderboard.scorekeeping.{Leaderboard, LeaderboardIdentifier}
import net.kolotyluk.scala.extras.Internalized

import scala.collection.concurrent.TrieMap

package object leaderboard {

  val nameToLeaderboardIdentifier = new TrieMap[String,LeaderboardIdentifier]
  val identifierToLeaderboard = new TrieMap[LeaderboardIdentifier,Leaderboard]
  val leaderboardEndpoint = new Endpoint

  // https://doc.akka.io/docs/akka-http/current/routing-dsl/path-matchers.html
  // https://github.com/spray/spray/blob/master/spray-routing/src/main/scala/spray/routing/PathMatcher.scala
  val base64identifier: PathMatcher1[InternalIdentifier[UUID]] = base64uuid flatMap { uuid => Some(Internalized(uuid)) }

}

package leaderboard {

  import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
  import spray.json.{DefaultJsonProtocol, PrettyPrinter}

  sealed trait EndpointResponse

  final case class Score(value: String, random: Long)

  final case class LeaderboardGetResponse(id: String, state: String, members: Long)

  final case class LeaderboardPostRequest(leaderboardName: Option[String], implementationName: String)

  final case class UpdateScoreRequest(leaderboardId: Option[String], memberId: Option[String], score: String)

  final case class LeaderboardPutScoresRequest(scores: Seq[UpdateScoreRequest])

  final case class LeaderboardPostResponse(name: Option[String], id: String)

  final case class LeaderboardStatusResponse(id: String, size: Int) extends EndpointResponse

  final case class LeaderboardStatusResponses(leaderboards: Seq[LeaderboardStatusResponse]) extends EndpointResponse

  final case class MemberStatusResponse(leaderboardId: String, memberId: String, score: Option[Score]) extends EndpointResponse

  trait JsonSupport extends SprayJsonSupport with DefaultJsonProtocol {

    implicit val scoreResponseFormat = jsonFormat2(Score)

    implicit val leaderboardGetResponseFormat = jsonFormat3(LeaderboardGetResponse)

    implicit val leaderboardPostRequestFormat = jsonFormat2(LeaderboardPostRequest)
    implicit val leaderboardPostResponseFormat = jsonFormat2(LeaderboardPostResponse)

    implicit val leaderboardStatusResponseFormat = jsonFormat2(LeaderboardStatusResponse)
    implicit val leaderboardStatusResponsesFormat = jsonFormat1(LeaderboardStatusResponses)

    implicit val memberStatusResponsesFormat = jsonFormat3(MemberStatusResponse)

    implicit val updateScoreRequestFormat = jsonFormat3(UpdateScoreRequest)

  }

  trait PrettyJasonSupport extends SprayJsonSupport with DefaultJsonProtocol {
    implicit val printer = PrettyPrinter
    implicit val errorPayloadFormat = jsonFormat4(ErrorPayload)
  }

  //case class LeaderboardRejection(response: HttpResponse) extends scala.AnyRef with Rejection {
  //}

  //final case class UnknownKindRejection()
  //  extends LeaderboardRejection(null) {
  //  def this(kind: String) {
  //    this(HttpResponse(BadRequest, entity = s"leaderboard.kind=$kind unknown! Specify one of: ConcurrentLeaderboard, LeaderboardActor, SynchronizedConcurrentLeaderboard, SynchronizedLeaderboard"))
  //  }
  //}

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

  class UnknownLeaderboardNameException(name: String)
    extends EndpointException(HttpResponse(NotFound,
      entity = s"leaderboard name=$name unknown")) {
  }
}
