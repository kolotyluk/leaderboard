package net.kolotyluk.leaderboard.scorekeeping

import java.util.concurrent.{ConcurrentHashMap, ConcurrentMap, ConcurrentNavigableMap, ConcurrentSkipListMap}
import java.util.{ConcurrentModificationException, UUID}

import net.kolotyluk.leaderboard.telemetry.Metrics
import net.kolotyluk.scala.extras.{Configuration, Identity, Logging}

import scala.annotation.tailrec
import scala.collection.concurrent.TrieMap
import scala.collection.mutable.ArrayBuffer
import scala.util.{Failure, Success, Try}

/** =Leaderboard Management=
  *
  */
object ConcurrentLeaderboard extends LeaderboardManager {
  val maximumSpinCountExceeded = "%s: This is probably caused because a lock was set on %s, but never removed, possibly because of thread failure."

  val nameToUuid = new TrieMap[String,UUID]
  val uuidToLeaderboard = new TrieMap[UUID,ConcurrentLeaderboard]

  /** =Add New Leaderboard=
    *
    * @return new Leaderboard instance
    */
  def add(): Try[ConcurrentLeaderboard] = add(None)

  def add(name: String): Try[ConcurrentLeaderboard] = {
    nameToUuid.get(name) match {
      case None =>
        add(Some(name)) match {
          case Failure(cause) => Failure(new Exception(s"Leaderboard '$name' cannot be created", cause))
          case Success(leaderboard) =>
            nameToUuid.put(name, leaderboard.getUuid)
            Success(leaderboard)
        }
      case Some(uuid) => Failure(new Exception(s"Leaderboard '$name' already exists"))
    }
  }

  def add(name: Option[String], uuid: UUID = UUID.randomUUID()): Try[ConcurrentLeaderboard] = {
    uuidToLeaderboard.get(uuid) match {
      case None =>
        // val memberToScore = new TrieMap[String,Option[Score]]
        // val memberToScore = new ConcurrentHashMap[String,Option[Score]]
        // val scoreToMember = new ConcurrentSkipListMap[Score,String]

      val leaderboard = new ConcurrentLeaderboard(new ConcurrentHashMap[String,Option[Score]], new ConcurrentSkipListMap[Score,String])
        uuidToLeaderboard.put(uuid, leaderboard) match {
          case None => Success(leaderboard)
          case Some(predecessor) =>
            // TODO Technically this cannot happen because we have already tested for it
            Failure(new Exception(s"Leaderboard $uuid already exists"))
        }
      case Some(leaderboard) => Failure(new Exception(s"Leaderboard $uuid already exists"))
    }
  }

  def get(uuid: UUID): Option[ConcurrentLeaderboard] = uuidToLeaderboard.get(uuid)

  def get(name: String): Option[ConcurrentLeaderboard] = {
    nameToUuid.get(name) match {
      case None => None
      case Some(uuid) => get(uuid)
    }
  }

  def getInfo(name: String): Option[Info] = {
    nameToUuid.get(name) match {
      case None => None
      case Some(uuid) => getInfo(uuid)
    }
  }

  def getInfo(uuid: UUID): Option[Info] = {

    uuidToLeaderboard.get(uuid) match {
      case None => None
      case Some(leaderboard) => Some(leaderboard.getInfo)
    }
  }
}

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
class ConcurrentLeaderboard(memberToScore: ConcurrentMap[String,Option[Score]], scoreToMember:  ConcurrentNavigableMap[Score,String])
  extends LeaderboardSync
    with Configuration
    with Logging {

  // val memberToScore = new TrieMap[String,Option[Score]]
  // val memberToScore = new ConcurrentHashMap[String,Option[Score]]
  // val scoreToMember = new ConcurrentSkipListMap[Score,String]

  override def getInfo = Info(uuid, Some(name), getCount)

  override def getUrlIdentifier(identifier: String) = Identity.getUrlIdentifier(identifier)

  override def getUrlIdentifier(uuid: UUID = UUID.randomUUID()) = Identity.getUrlIdentifier(uuid)

  override def delete(member: String) = {
    delete(member, 0)
  }

  @tailrec
  private def delete(member: String, spinCount: Long): Boolean = {
    try {
      Metrics.checkSpinCount(member, spinCount)
    } catch {
      case cause: ConcurrentModificationException =>
        throw new ConcurrentModificationException(ConcurrentLeaderboard.maximumSpinCountExceeded.format("delete", member), cause)
    }

    // Set the spin-lock
    put(member, None) match {
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

  /** =Fancy Put=
    * Fancy way to put a member on the leaderboard to support spin-locks
    * @param member
    * @param score
    * @return
    */
  def put(member: String, score: Option[Score]) = {
    val value = memberToScore.put(member, None)
    if (value == null) None else Some(value)
  }

  def getCount = {
    memberToScore.size
  }

  override def getName = Some(name)

  override def getUuid = uuid

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

  override def getScore(member: String): Option[BigInt] = {
    getScore(member, 0)
  }

  @tailrec
  final def getScore(member: String, spinCount: Long): Option[BigInt] = {
    try {
      Metrics.checkSpinCount(member, spinCount)
    } catch {
      case cause: ConcurrentModificationException =>
        throw new ConcurrentModificationException(ConcurrentLeaderboard.maximumSpinCountExceeded.format("getScore", member), cause)
    }


    get(member) match {
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

  def get(member: String): Option[Option[Score]] = {
    val value = memberToScore.get(member)
    if (value == null) None else Some(value)
  }

  override def getStanding(member: String) = {
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
    try {
      Metrics.checkSpinCount(member, spinCount)
    } catch {
      case cause: ConcurrentModificationException =>
        throw new ConcurrentModificationException(ConcurrentLeaderboard.maximumSpinCountExceeded.format("getStanding", member), cause)
    }

    get(member) match {
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
  override def update(mode: UpdateMode, member: String, value: BigInt) = {
    update(mode, member, Score(value))
  }

  override def update(mode: UpdateMode, member: String, newScore: Score) = {
    updater(mode, member, newScore, 0, System.nanoTime)
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
    * <p>
    * Under extremely high loads of 130,000 transactions-per-second or move, using 12 logical processors,
    * a single transaction can spin over 200 times, for over 12 milliseconds (see unit tests). While this
    * is a worst case test scenario, it is a decent benchmark of worst case performance.
    *
    * @param member member ID
    * @param newScore existing score created by another ScoreKeeper
    */
  @tailrec
  private def updater(mode: UpdateMode, member: String, newScore: Score, spinCount: Int, spinStart: Long): Score = {
    // Caution: there is some subtle logic below, so don't modify it unless you grok it

    try {
      Metrics.checkSpinCount(member, spinCount)
    } catch {
      case cause: ConcurrentModificationException =>
        throw new ConcurrentModificationException(ConcurrentLeaderboard.maximumSpinCountExceeded.format("update", member), cause)
    }

    // Set the spin-lock
    put(member, None) match {
      case None =>
        // BEGIN CRITICAL SECTION
        // Member's first time on the board
        if (scoreToMember.put(newScore, member) != null) {
          val message = s"$member: added new member in memberToScore, but found old member in scoreToMember"
          logger.error(message)
          throw new ConcurrentModificationException(message)
        }
        memberToScore.put(member, Some(newScore)) // remove the spin-lock
        newScore
        // END CRITICAL SECTION
      case Some(option) => option match {
        case None =>            // Update in progress, so spin until complete
          //logger.debug(s"update: $member locked, spinCount = $spinCount")
          // Waste time so the lock holder can finish, by yielding the processor to other threads
          // Exponential back-off seems to offer the best behavior
          for (i <- -1 to spinCount * spinCount) Thread.`yield`
          updater(mode, member, newScore, spinCount + 1, spinStart)
        case Some(oldScore) =>
          try { // BEGIN CRITICAL SECTION
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
          } catch {
            case cause: Throwable =>
              // Unlikely to get here, but just in case, delete the member, which should also delete the lock,
              // and prevent spinning forever.
              scoreToMember.remove(oldScore)
              memberToScore.remove(member)
              logger.error(s"Exception in critical section. Removing member $member", cause)
          } // END CRITICAL SECTION
          // Do this outside the critical section to reduce time under lock
          if (spinCount > 0) Metrics.checkSpinTime(System.nanoTime() - spinStart)
          newScore
      }
    }
  }

}
