package net.kolotyluk.leaderboard.scorekeeping

import java.util.UUID
import java.util.concurrent.{ConcurrentMap, ConcurrentNavigableMap}

import net.kolotyluk.scala.extras.{Identity, Logging}

class SynchronizedConcurrentLeaderboard(
    memberToScore: ConcurrentMap[String,Option[Score]],
    scoreToMember: ConcurrentNavigableMap[Score,String]
  ) extends LeaderboardSync with Logging {

  val consecutiveLeaderboard = new ConsecutiveLeaderboard(memberToScore, scoreToMember)

  override def delete(member: String) = {

    member.intern().synchronized {
      consecutiveLeaderboard.delete(member)
    }
  }

  override def getCount = consecutiveLeaderboard.getCount

  override def getInfo = consecutiveLeaderboard.getInfo

  override def getName = consecutiveLeaderboard.getName

  override def getRange(start: Long, stop: Long) = {
    //memberToScore.synchronized {
      consecutiveLeaderboard.getRange(start, stop)
    //}
  }

  override def getScore(member: String) = {
    member.intern().synchronized {
      consecutiveLeaderboard.getScore(member)
    }
  }

  override def getStanding(member: String) = {
    member.intern().synchronized {
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
    member.intern().synchronized {
      consecutiveLeaderboard.update(mode, member, newScore)
    }
  }
}
