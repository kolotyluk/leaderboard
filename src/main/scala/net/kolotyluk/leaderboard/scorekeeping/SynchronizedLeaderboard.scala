package net.kolotyluk.leaderboard.scorekeeping

import java.util
import java.util.{Map, UUID}

import akka.Done
import net.kolotyluk.scala.extras.{Identity, Logging}

import scala.collection.mutable.ArrayBuffer

class SynchronizedLeaderboard(
    memberToScore: Map[String,Option[Score]],
    scoreToMember: util.NavigableMap[Score,String]
  ) extends LeaderboardSync with Logging {

  val consecutiveLeaderboard = new ConsecutiveLeaderboard(memberToScore, scoreToMember)

  override def delete(member: String) = {
    memberToScore.synchronized {
      consecutiveLeaderboard.delete(member)
    }
  }

  override def getCount = consecutiveLeaderboard.getCount

  override def getInfo = consecutiveLeaderboard.getInfo

  override def getName = consecutiveLeaderboard.getName

  override def getRange(start: Long, stop: Long) = {
    memberToScore.synchronized {
      consecutiveLeaderboard.getRange(start, stop)
    }
  }

  override def getScore(member: String) = {
    memberToScore.synchronized {
      consecutiveLeaderboard.getScore(member)
    }
  }

  override def getStanding(member: String) = {
    memberToScore.synchronized {
      consecutiveLeaderboard.getStanding(member)
    }
  }

  override def getUuid = consecutiveLeaderboard.getUuid

  override def getUrlIdentifier(identifier: String) = consecutiveLeaderboard.getUrlIdentifier(identifier)

  override def getUrlIdentifier(uuid: UUID = UUID.randomUUID()) = Identity.getUrlIdentifier(uuid)

  override def update(mode: UpdateMode, member: String, value: BigInt) = {
    val score = Score(value, randomLong)
    update(mode, member, score)
  }

  override def update(mode: UpdateMode, member: String, newScore: Score) = {
    memberToScore.synchronized {
      consecutiveLeaderboard.update(mode, member, newScore)
    }
  }
}
