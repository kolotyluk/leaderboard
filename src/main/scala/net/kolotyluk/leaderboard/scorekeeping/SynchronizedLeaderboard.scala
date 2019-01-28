package net.kolotyluk.leaderboard.scorekeeping

import java.util
import java.util.{Map, UUID}

import akka.Done
import net.kolotyluk.scala.extras.{Identity, Logging}

import scala.collection.mutable.ArrayBuffer

class SynchronizedLeaderboard(
    leaderboardIdentifier: LeaderboardIdentifier,
    memberToScore: Map[MemberIdentifier,Option[Score]],
    scoreToMember: util.NavigableMap[Score,MemberIdentifier]
  ) extends LeaderboardSync with Logging {

  val consecutiveLeaderboard = new ConsecutiveLeaderboard(leaderboardIdentifier, memberToScore, scoreToMember)

  override def delete(memberIdentifier: MemberIdentifier) = {
    memberToScore.synchronized {
      consecutiveLeaderboard.delete(memberIdentifier)
    }
  }

  override def getCount = consecutiveLeaderboard.getCount

  override def getIdentifier = leaderboardIdentifier

  override def getInfo = consecutiveLeaderboard.getInfo

  override def getName = consecutiveLeaderboard.getName

  override def getRange(start: Long, stop: Long) = {
    memberToScore.synchronized {
      consecutiveLeaderboard.getRange(start, stop)
    }
  }

  override def getScore(memberIdentifier: MemberIdentifier) = {
    memberToScore.synchronized {
      consecutiveLeaderboard.getScore(memberIdentifier)
    }
  }

  override def getStanding(memberIdentifier: MemberIdentifier) = {
    memberToScore.synchronized {
      consecutiveLeaderboard.getStanding(memberIdentifier)
    }
  }

  override def update(mode: UpdateMode, memberIdentifier: MemberIdentifier, value: BigInt) = {
    val score = Score(value, randomLong)
    update(mode, memberIdentifier, score)
  }

  override def update(mode: UpdateMode, memberIdentifier: MemberIdentifier, newScore: Score) = {
    memberToScore.synchronized {
      consecutiveLeaderboard.update(mode, memberIdentifier, newScore)
    }
  }
}
