package net.kolotyluk.leaderboard.scorekeeping
import java.util
import java.util.{Map, UUID}

import net.kolotyluk.scala.extras.{Identity, Logging}

import scala.collection.mutable.ArrayBuffer

/** =Fast Unsafe Leaderboard=
  * Non-threadsafe leaderboard
  * <p>
  * This leaderboard is not thread-safe, and intended to be uses inside an Akka Actor.
  */
class ConsecutiveLeaderboard(
    override val leaderboardIdentifier: LeaderboardIdentifier,
    memberToScore: Map[MemberIdentifier,Option[Score]],
    scoreToMember: util.NavigableMap[Score,MemberIdentifier]
  ) extends LeaderboardSync with Logging {

  override def delete(memberIdentifier: MemberIdentifier) = {
    memberToScore.get(memberIdentifier) match {
      case null =>
        logger.warn(s"member = $memberIdentifier not found on leaderboard $name")
        false
      case None =>
        memberToScore.remove(memberIdentifier)
        true
      case Some(score) =>
        scoreToMember.remove(score)
        memberToScore.remove(memberIdentifier)
        true
    }
  }

  override def getCount = memberToScore.size

  override def getIdentifier = leaderboardIdentifier

  override def getInfo = LeaderboardInfo(leaderboardIdentifier, Some(name), memberToScore.size())

  override def getName = Some(name)

  override def getRange(start: Long, stop: Long) = {

    val totalCount = getCount

    val scoreSet = scoreToMember.entrySet().iterator()

    val placings = new ArrayBuffer[Placing]

    var place: Long = 0

    while (place <= stop) {
      if (scoreSet.hasNext) {
        val entry = scoreSet.next()
        if (place >= start) placings.append(Placing(entry.getValue, entry.getKey.value, place))
        place += 1
      } else place = stop +1
    }

    Range(placings, totalCount)
  }

  override def getScore(member: MemberIdentifier) = {
    memberToScore.get(member) match {
      case null =>
        None
      case score: Option[Score] =>
        score
    }
  }

  /** =Compute Standing=
    * <p>
    * Iterate through the map to compute member standing
    * <p>
    *
    * @param member
    * @return None if member not present, Some[Standing] otherwise
    */
  override def getStanding(memberIdentifier: MemberIdentifier) = {

    memberToScore.get(memberIdentifier) match {
      case null =>
        None
      case None =>
        None
      case Some(score) =>
        val scores = scoreToMember.keySet().iterator()
        var count: Int = 0
        var place: Int = 0
        while (scores.hasNext) {
          count += 1
          val key = scores.next()
          if (key == score) place = count
        }
        assert(count > 0, "scoreToMember is empty")
        Some(Standing(place, count))
    }
  }

  //def put(member: String, score: Option[Score]): Option[Option[Score]] = ???

  override def update(mode: UpdateMode, memberIdentifier: MemberIdentifier, value: BigInt) = {
    val score = Score(value, randomLong)
    update(mode, memberIdentifier, score)
  }

  override def update(mode: UpdateMode, memberIdentifier: MemberIdentifier, theScore: Score) = {

    /** =Set Initial Score=
      * Assumes old score = 0
      *
      * @param member
      * @param InitialScore
      */
    def setInitialScore(memberIdentifier: MemberIdentifier, InitialScore: Score) = {
      // logger.debug(s"setInitialScore: member = $member, score = $score")
      scoreToMember.put(InitialScore, memberIdentifier)
      memberToScore.put(memberIdentifier, Some(InitialScore))
      InitialScore
    }

    /** =Add Existing Score=
      *
      * @param member
      * @param oldScore
      * @param newScore
      */
    def setScore(memberIdentifier: MemberIdentifier, oldScore: Score, newScore: Score) = {
      if (oldScore == newScore || oldScore.equals(theScore)) oldScore
      else {
        scoreToMember.remove(oldScore)
        scoreToMember.put(newScore, memberIdentifier)
        memberToScore.put(memberIdentifier, Some(newScore))
        newScore
      }
    }

    memberToScore.get(memberIdentifier) match {
      case null | None =>
        setInitialScore(memberIdentifier, theScore)
      case Some(oldScore) =>
        mode match {
          case Replace =>
            setScore(memberIdentifier, oldScore, theScore)
          case Increment =>
            // Reuse the old random so we don't waste time computing another.
            val sumScore = Score(oldScore.value + theScore.value, theScore.random)
            setScore(memberIdentifier, oldScore, sumScore)
        }
    }
  }

}
