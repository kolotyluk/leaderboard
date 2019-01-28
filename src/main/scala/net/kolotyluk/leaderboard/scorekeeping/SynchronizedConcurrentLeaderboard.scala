package net.kolotyluk.leaderboard.scorekeeping

import java.util.UUID
import java.util.concurrent.{ConcurrentMap, ConcurrentNavigableMap}

import net.kolotyluk.scala.extras.{Identity, Logging}

class SynchronizedConcurrentLeaderboard(
    leaderboardIdentifier: LeaderboardIdentifier,
    memberToScore: ConcurrentMap[MemberIdentifier,Option[Score]],
    scoreToMember: ConcurrentNavigableMap[Score,MemberIdentifier]
  ) extends LeaderboardSync with Logging {

  val consecutiveLeaderboard = new ConsecutiveLeaderboard(leaderboardIdentifier, memberToScore, scoreToMember)

  override def delete(memberIdentifier: MemberIdentifier) = {

    memberIdentifier.synchronized {
      consecutiveLeaderboard.delete(memberIdentifier)
    }
  }

  override def getCount = consecutiveLeaderboard.getCount

  override def getIdentifier = leaderboardIdentifier

  override def getInfo = consecutiveLeaderboard.getInfo

  override def getName = consecutiveLeaderboard.getName

  override def getRange(start: Long, stop: Long) = {
    //memberToScore.synchronized {
      consecutiveLeaderboard.getRange(start, stop)
    //}
  }

  override def getScore(memberIdentifier: MemberIdentifier) = {
    memberIdentifier.synchronized {
      consecutiveLeaderboard.getScore(memberIdentifier)
    }
  }

  override def getStanding(memberIdentifier: MemberIdentifier) = {
    memberIdentifier.synchronized {
      consecutiveLeaderboard.getStanding(memberIdentifier)
    }
  }

  override def update(mode: UpdateMode, memberIdentifier: MemberIdentifier, value: BigInt) = {
    val score = Score(value, randomLong)
    update(mode, memberIdentifier, score)
  }

  override def update(mode: UpdateMode, memberIdentifier: MemberIdentifier, newScore: Score) = {
    memberIdentifier.synchronized {
      consecutiveLeaderboard.update(mode, memberIdentifier, newScore)
    }
  }
}
