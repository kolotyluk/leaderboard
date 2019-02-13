package net.kolotyluk.leaderboard.Akka.endpoint.leaderboard
import java.util.concurrent.{ConcurrentHashMap, ConcurrentSkipListMap}
import java.util.{HashMap, TreeMap, UUID}

import net.kolotyluk.leaderboard.Akka.endpoint
import net.kolotyluk.leaderboard.Akka.endpoint.leaderboard.failure.DupicateLeaderboardIdentifierError
import net.kolotyluk.leaderboard.scorekeeping.{ConcurrentLeaderboard, Leaderboard, LeaderboardIdentifier, MemberIdentifier, Score, SynchronizedConcurrentLeaderboard, SynchronizedLeaderboard}
import net.kolotyluk.scala.extras.{Internalized, Logging}

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.implicitConversions

import scala.language.postfixOps


/** =Leaderboard Implementation Enumeration=
  *
  * Being a research project, there are a number of different implementations of the same API, using different
  * data structures, so that we can compare and contrast performance characteristics and select the best solutions.
  */
object Implementation extends Enumeration with Logging {

  protected case class Val(constructor: LeaderboardIdentifier => Leaderboard) extends super.Val {
    def create(nameOption: Option[String]): LeaderboardPostResponse = {
      val leaderboardIdentifier = Internalized[UUID](UUID.randomUUID)
      val leaderboardUrlIdentifier = endpoint.internalIdentifierToUrlId(leaderboardIdentifier)
      nameOption match {
        case None =>
        case Some(leaderboardName) =>
          nameToLeaderboardIdentifier.put(leaderboardName,leaderboardIdentifier )
      }
      identifierToLeaderboard.put(leaderboardIdentifier,constructor(leaderboardIdentifier)) match {
        case None =>
          LeaderboardPostResponse(nameOption, leaderboardUrlIdentifier)
        case Some(_) =>
          throw new DupicateLeaderboardIdentifierError(leaderboardIdentifier)
      }
    }
  }

  implicit def valueToImplementationVal(x: Value): Val = x.asInstanceOf[Val]

  val ConcurrentLeaderboard = Val(
    leaderboardIdentifier => {
      val memberToScore = new ConcurrentHashMap[MemberIdentifier,Option[Score]]
      val scoreToMember = new ConcurrentSkipListMap[Score,MemberIdentifier]
      new ConcurrentLeaderboard(leaderboardIdentifier, memberToScore, scoreToMember)
    }
  )

  val LeaderboardActor = Val(
    leaderboardIdentifier => {
      // val memberToScore = new HashMap[MemberIdentifier,Option[Score]]
      // val scoreToMember = new TreeMap[Score,MemberIdentifier]
      // val leaderboard = new ConsecutiveLeaderboard(leaderboardIdentifier, memberToScore, scoreToMember)
      val leaderboardFuture = net.kolotyluk.leaderboard.Akka.leaderboardManagerActor.create(leaderboardIdentifier)
      Await.result(leaderboardFuture, 10 seconds)
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
      val memberToScore = new HashMap[MemberIdentifier, Option[Score]]
      val scoreToMember = new TreeMap[Score, MemberIdentifier]
      new SynchronizedLeaderboard(leaderboardIdentifier, memberToScore, scoreToMember)
    }
  )
}