package net.kolotyluk.leaderboard.scorekeeping

import java.util.ConcurrentModificationException
import java.util.concurrent.ConcurrentSkipListMap

import net.kolotyluk.scala.extras.{Configuration, Logging}

import scala.annotation.tailrec
import scala.collection.concurrent.TrieMap
import scala.collection.mutable.ArrayBuffer

/** =Leaderboard Score Keeping=
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
  *
  * @author eric@kolotyluk.net
  */
class Leaderboard extends Configuration with Logging {

  val memberToScore = new TrieMap[String,Option[Score]]
  val scoreToMember = new ConcurrentSkipListMap[Score,String]

  def delete(member: String): Boolean = {
    delete(member, 0)
  }

  @tailrec
  private def delete(member: String, spinCount: Long): Boolean = {
    if (spinCount > msximumSpinCount) {
      throw new ConcurrentModificationException(maximumSpinCountExceeded.format("delete", msximumSpinCount, member))
    }

    // Set the spin-lock
    memberToScore.put(member, None) match {
      case None =>    // already deleted
        false
      case Some(option) => option match {
        case None =>  // Update in progress, so spin until complete
          //logger.debug(s"delete: $member locked")
          //Thread.sleep(1)
          delete(member, spinCount + 1)
        case Some (oldScore) => // CRITICAL SECTION
          scoreToMember.remove (oldScore)
          memberToScore.remove (member) // removes lock too
          true
      }
    }
  }

  def getCount() = {
    memberToScore.size
  }

  /** =Get Range of Scores=
    * <p>
    * Return a range of scores from start to stop
    * <p>
    *
    *
    * @param start
    * @param stop
    * @return
    * @throws IndexOutOfBoundsException if more than Int.MaxValue scores in the result
    */
  def getRange(start: Long, stop: Long) = {

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

  def getScore(member: String): Option[BigInt] = {
    getScore(member, 0)
  }

  @tailrec
  final def getScore(member: String, spinCount: Long): Option[BigInt] = {
    if (spinCount > msximumSpinCount) {
      throw new ConcurrentModificationException(maximumSpinCountExceeded.format("getScore", msximumSpinCount, member))
    }

    memberToScore.get(member) match {
      case None => None
      case Some(option) => option match {
        case None => // Update in progress, so spin until complete
          //logger.debug(s"getScore: $member locked")
          //Thread.sleep(1)
          getScore(member, spinCount + 1)
        case Some(score) => // CRITICAL SECTION
          Some(score.value)
      }
    }
  }

  def getStanding(member: String): Option[Standing] = {
    getStanding(member, 0)
  }

  /** =Compute Standing=
    * <p>
    * Iterate through the map to compute member standing
    * <p>
    * ''Warning:'' Results may not be accurate if leaderboard is being updated while standing is being computed.
    * @param member
    * @return None if member not present, Some[Standing] otherwise
    */
  def getStanding(member: String, spinCount: Long): Option[Standing] = {
    if (spinCount > msximumSpinCount) {
      throw new ConcurrentModificationException(maximumSpinCountExceeded.format("getStanding", msximumSpinCount, member))
    }

    memberToScore.get(member) match {
      case None => None     // Member not on leaderboard
      case Some(option) => option match {
        case None =>        // Update in progress, so spin until complete
          getStanding(member, spinCount + 1)
        case Some(score) => // CRITICAL SECTION
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
  }

  /** Update Leaderboard with New Score
    *
    * @param member
    * @param value
    */
  def update(mode: UpdateMode, member: String, value: BigInt): Unit = {
    update(mode, member, Score(value))
  }

  def update(mode: UpdateMode, member: String, newScore: Score): Unit = {
    update(mode, member, newScore, 0)
  }

  /** =Update Leaderboard with Existing Score=
    * <p>
    * This should only be called from another ScoreKeeper, running in a different Akka Cluster Node.
    * <p>
    * Performance is a little better for Replace than Increment, but for both cases, performance is O(log n).
    * <p>
    * ==Implementation Notes==
    * ===Spin Lock===
    * This implementation uses a [[https://en.wikipedia.org/wiki/Spinlock spin-lock]] to prevent
    * [[https://en.wikipedia.org/wiki/Race_condition race-conditions]]. While both [[memberToScore]] and
    * [[scoreToMember]] are concurrent data structures, they need to be updated in tandem for the same
    * member update.
    *
    * @param member member ID
    * @param newScore existing score created by another ScoreKeeper
    */
  @tailrec
  private def update(mode: UpdateMode, member: String, newScore: Score, spinCount: Long): Unit = {
    // Caution: there is some subtle logic below, so don't modify it unless you grok it

    if (spinCount > msximumSpinCount) {
      throw new ConcurrentModificationException(maximumSpinCountExceeded.format("update", msximumSpinCount, member))
    }

    // Set the spin-lock
    memberToScore.put(member, None) match {
      case None =>              // CRITICAL SECTION
        // Member's first time on the board
        if (scoreToMember.put(newScore, member) != null) {
          val message = s"$member: added new member in memberToScore, but found old member in scoreToMember"
          logger.error(message)
          throw new ConcurrentModificationException(message)
        }
        memberToScore.put(member, Some(newScore)) // remove the spin-lock
      case Some(option) => option match {
        case None =>            // Update in progress, so spin until complete
          // TODO prevent infinite loop
          logger.debug(s"update: $member locked, spinCount = $spinCount")
          //Thread.sleep(1)
          update(mode, member, newScore, spinCount + 1)
        case Some(oldScore) =>  // CRITICAL SECTION
          // Member already on the leaderboard
          if (scoreToMember.remove(oldScore) == null) {
              val message = s"$member: oldScore not found in scoreToMember, concurrency defect"
              logger.error(message)
              throw new ConcurrentModificationException(message)
            } else {
              val score =
                mode match {
                  case Replace =>
                    //logger.debug(s"$member: newScore = $newScore")
                    newScore
                  case Increment =>
                    //logger.debug(s"$member: newScore = $newScore, oldScore = $oldScore")
                    Score(newScore.value + oldScore.value)
                }
              //logger.debug(s"$member: updated score = $score")
              scoreToMember.put(score, member)
              memberToScore.put(member, Some(score))  // remove the spin-lock
              //logger.debug(s"update: $member unlocked")
            }
      }
    }
  }

}
