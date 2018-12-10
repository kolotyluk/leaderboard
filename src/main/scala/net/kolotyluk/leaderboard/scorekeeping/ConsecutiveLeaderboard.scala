package net.kolotyluk.leaderboard.scorekeeping
import java.util
import java.util.{Map, UUID}

import net.kolotyluk.scala.extras.{Identity, Logging}

import scala.collection.mutable.ArrayBuffer

class ConsecutiveLeaderboard(memberToScore: Map[String,Option[Score]], scoreToMember: util.NavigableMap[Score,String]) extends Leaderboard with Logging {

  val uuid = UUID.randomUUID()
  var name = uuid.toString

  override def delete(member: String): Boolean = {
    memberToScore.get(member) match {
      case null =>
        logger.warn(s"member = $member not found on leaderboard $name")
        false
      case None =>
        memberToScore.remove(member)
        true
      case Some(score) =>
        scoreToMember.remove(score)
        memberToScore.remove(member)
        true
    }
  }

  override def getCount: Int = memberToScore.size

  override def getInfo: Info = Info(uuid, Some(name), memberToScore.size())

  override def getName: Option[String] = Some(name)

  /** =Get Range of Scores=
    * <p>
    * Return a range of scores from start to stop
    * <p>
    *
    * @param start
    * @param stop
    * @return
    * @throws IndexOutOfBoundsException if more than Int.MaxValue scores in the result
    */
  override def getRange(start: Long, stop: Long): Range = {

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

  override def getScore(member: String): Option[BigInt] = {
    memberToScore.get(member) match {
      case null =>
        None
      case None =>
        None
      case Some(score) =>
        Some(score.value)
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
  override def getStanding(member: String): Option[Standing] = {

    memberToScore.get(member) match {
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

  override def getUuid: UUID = uuid

  override def getUrlIdentifier(identifier: String): UUID = Identity.getUrlIdentifier(identifier)

  override def getUrlIdentifier(uuid: UUID = UUID.randomUUID()): String = Identity.getUrlIdentifier(uuid)

  //def put(member: String, score: Option[Score]): Option[Option[Score]] = ???

  override def setName(name: Option[String]): Unit = ???

  override def update(mode: UpdateMode, member: String, value: BigInt): Unit = {
    val score = Score(value, randomLong)
    update(mode, member, score)
  }

  /** =Update Leaderboard with Existing Score=
    * <p>
    * This should only be called from another ScoreKeeper, running in a different Akka Cluster Node.
    * <p>
    * Performance is a little better for Replace than Increment, but for both cases, performance is O(log n).
    * <p>
    *
    * @param member   member ID
    * @param newScore existing score created by another ScoreKeeper
    */
  override def update(mode: UpdateMode, member: String, newScore: Score): Unit = {

    /** =Add Existing Score=
      *
      * @param member
      * @param oldScore
      * @param newScore
      */
    def addScore(member: String, oldScore: Score, newScore: Score): Unit = {
      scoreToMember.remove(oldScore)
      scoreToMember.put(newScore, member)
      memberToScore.put(member, Some(newScore))
    }

    /** =Set Initial Score=
      *
      * @param member
      * @param score
      */
    def setScore(member: String, score: Score): Unit = {
      scoreToMember.put(score, member)
      memberToScore.put(member, Some(score))
    }

    memberToScore.get(member) match {
      case null | None =>
        setScore(member, newScore)
      case Some(score) =>
        if (score.value != newScore.value || score.random != newScore.random) {
          mode match {
            case Increment =>
              addScore(member, score, Score(score.value + newScore.value, newScore.random))
            case Replace =>
              addScore(member, score, newScore)
          }
      }
    }
  }
}
