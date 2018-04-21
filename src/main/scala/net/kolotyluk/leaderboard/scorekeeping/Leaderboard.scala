package net.kolotyluk.leaderboard.scorekeeping

import java.util.concurrent.ConcurrentSkipListMap

import net.kolotyluk.scala.extras.Logging

import scala.collection.concurrent.TrieMap

sealed trait Mode
case object Increment extends Mode
case object Replace extends Mode

/** =Track Leaderboard Scores=
  * <p>
  * Keep track of leaderboard scores.
  * <p>
  * ==Redis==
  * Many leaderboard implementations use Redis Sorted Sets for keeping track of scores. In general
  * this is an excellent way to implement a leaderboard, but there are some issues with this.
  * ===Floating Point Scores===
  * Redis uses a [[https://en.wikipedia.org/wiki/Double-precision_floating-point_format 64-bit floating point number]]
  * for scores. The main problem with this is that this
  * puts an upper limit on scorekeeping because beyond 52 bits of precision, it is no longer possible
  * to distinguish unique scores. This implementation uses BigInt, which has infinite precision, so
  * there are no bounds on how high (or low) scores can get.
  * ===Tie Breaking===
  * The next problem is tie-breaking, which relies on the lexical ordering of member IDs. This is
  * intrinsically unfair because it means the same members will alway win in a tie-breaking situation.
  * Instead, tie-breaking here is done based on random numbers, such that the largest random number
  * breaks the tie.
  * ==Random Numbers==
  * Timestamps, UUIDs, and other methods were considered for tie-breaking, but this design allows
  * for multiple ScoreKeepers on multiple Akka Cluster Nodes, and timestamps and UUIDs would lead
  * to different nodes giving tie-breaking preference over other nodes. Random numbers are 64-bits,
  * so there is a 1 in 2^64^ chance of a collision, and a failed tie breaking. Random seeds are
  * created by calling System.nanoTime each time a new random number is generated.
  * <p>
  * =Threadsafe=
  * This code is intended to be threadsafe, using concurrent non-blocking data structures, in order
  * to offer better performance over serialization in a single actor.
  */
class ScoreKeeper extends Logging {

  val memberToScore = new TrieMap[String,Score]
  val scoreToMember = new ConcurrentSkipListMap[Score,String]


  //set.put(10, "Fred")

  /** Update Leaderboard with New Score
    *
    * @param member
    * @param value
    */
  def update(mode: Mode, member: String, value: BigInt): Unit = {
    update(mode, member, Score(value))
  }

  /** =Update Leaderboard with Existing Score=
    * <p>
    * This should only be called from another ScoreKeeper, running in a different Akka Cluster Node.
    * <p>
    * Performance is a little better for Replace than Increment, but for both cases, performance is O(log n).
    *
    * @param member
    * @param newScore existing score created by another ScoreKeeper
    */
  def update(mode: Mode, member: String, newScore: Score): Unit = {
    // Caution: there is some subtle logic below, so don't modify it until you grok it

    memberToScore.put(member, newScore) match {

      case None => // Member's first time on the board
        assert(scoreToMember.put(newScore, member) == null, {
            // Possible concurrency defect?
            val message = "ScoreKeeper inconsistent, added new member in memberToScore, but found old member in scoreToMember"
            logger.error(message)
            message
          }
        )

      case Some(oldScore) => // Member already on the board
        // TODO convince myself this will be consistent from multiple threads..., esp. wrt delete
        val old = scoreToMember.remove(oldScore)
        if (old == null) {
          // TODO this should not happen
          logger.error("oldScore not found in scoreToMember")
        } else {
          val score =
          mode match {
            case Replace =>
              logger.debug(s"newScore = $newScore")
              newScore
            case Increment =>
              logger.debug(s"newScore = $newScore, oldScore = $oldScore")
              val score = Score(newScore.value + oldScore.value)
              memberToScore.put(member, score)
              score

          }
          logger.debug(s"updated score = $score")
          scoreToMember.put(score, member)
        }
    }
  }

  def getScore(member: String): Option[BigInt] = {
    memberToScore.get(member) match {
      case None => None
      case Some(score) => Some(score.value)
    }
  }

  /** =Compute Standing=
    * <p>
    * Iterate through the map to compute member standing
    * <p>
    * ''Warning:'' Results may not be accurate if leaderboard is being updated while standing is being computed.
    * @param member
    * @return None if member not present, Some[Standing] otherwise
    */
  def getStanding(member: String): Option[Standing] = {
    memberToScore.get(member) match {
      case None => None
      case Some(score) =>
        val m = scoreToMember.keySet().iterator()
        var count: Int = 0
        var place: Int = 0
        while (m.hasNext) {
          count += 1
          val key = m.next()
          if (key == score) place = count
        }
        assert(count > 0, "scoreToMember is empty")
        Some(Standing(place, count))
    }
  }

  def getCount() = {
    memberToScore.size
  }

  def delete(member: String) = {

    // We need to synchronize (lock) our progress through here because this could lead to inconsistent
    // concurrent behavior during update. Note we intern the member string so that it is a unique object
    // before setting a lock on it.

    // TODO make sure this is still atomically correct wrt update

    member.intern.synchronized{
      memberToScore.remove(member) match {
        case None =>
        case Some(oldScore) => scoreToMember.remove(oldScore)
      }
    }

  }
}
