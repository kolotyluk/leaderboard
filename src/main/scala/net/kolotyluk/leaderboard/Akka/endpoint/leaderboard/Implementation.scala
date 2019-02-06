package net.kolotyluk.leaderboard.Akka.endpoint.leaderboard

import java.util.concurrent.{ConcurrentHashMap, ConcurrentSkipListMap}
import java.util.{HashMap, TreeMap, UUID}

import net.kolotyluk.leaderboard.Akka.{LeaderboardActor, endpoint}
import net.kolotyluk.leaderboard.scorekeeping.{ConcurrentLeaderboard, ConsecutiveLeaderboard, Leaderboard, LeaderboardIdentifier, MemberIdentifier, Score, SynchronizedConcurrentLeaderboard, SynchronizedLeaderboard}
import net.kolotyluk.scala.extras.Internalized

import scala.language.implicitConversions

/** =Leaderboard Implementation Enumeration=
  *
  * As a research project, there are a number of different implementations of the basic data structures so that we can
  * compare and contrast performance characteristics.
  */
object Implementation extends Enumeration {

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
      val memberToScore = new HashMap[MemberIdentifier,Option[Score]]
      val scoreToMember = new TreeMap[Score,MemberIdentifier]
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
      val memberToScore = new HashMap[MemberIdentifier, Option[Score]]
      val scoreToMember = new TreeMap[Score, MemberIdentifier]
      new SynchronizedLeaderboard(leaderboardIdentifier, memberToScore, scoreToMember)
    }
  )
}