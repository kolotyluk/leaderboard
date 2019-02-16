package net.kolotyluk.leaderboard.scorekeeping

import java.util.concurrent.{ConcurrentHashMap, ConcurrentMap, ConcurrentNavigableMap, ConcurrentSkipListMap}
import java.util.{ConcurrentModificationException, UUID}

import net.kolotyluk.leaderboard.scorekeeping
import net.kolotyluk.leaderboard.telemetry.Metrics
import net.kolotyluk.scala.extras.{Configuration, Internalized, Logging}

import scala.annotation.tailrec
import scala.collection.concurrent.TrieMap
import scala.collection.mutable.ArrayBuffer
import scala.util.{Failure, Random, Success, Try}

/** =Leaderboard Management=
  *
  */
object ConcurrentLeaderboard extends LeaderboardManager {
  val maximumSpinCountExceeded = "%s: This is probably caused because a lock was set on %s, but never removed, possibly because of thread failure."

  val nameToId = new TrieMap[String,LeaderboardIdentifier]
  val idToLeaderboard = new TrieMap[LeaderboardIdentifier,ConcurrentLeaderboard]

  /** =Add New Leaderboard=
    *
    * @return new Leaderboard instance
    */
  def add(): Try[ConcurrentLeaderboard] = add(None,Internalized(UUID.randomUUID()))

  def add(name: String): Try[ConcurrentLeaderboard] = {
    nameToId.get(name) match {
      case None =>
        add(Some(name), Internalized(UUID.randomUUID())) match {
          case Failure(cause) => Failure(new Exception(s"Leaderboard '$name' cannot be created", cause))
          case Success(leaderboard) =>
            nameToId.put(name, leaderboard.leaderboardIdentifier)
            Success(leaderboard)
        }
      case Some(_) => Failure(new Exception(s"Leaderboard '$name' already exists"))
    }
  }

  def add(name: Option[String], leaderboardIdentifier: LeaderboardIdentifier): Try[ConcurrentLeaderboard] = {
    idToLeaderboard.get(leaderboardIdentifier) match {
      case None =>
        // val memberToScore = new TrieMap[String,Option[Score]]
        // val memberToScore = new ConcurrentHashMap[String,Option[Score]]
        // val scoreToMember = new ConcurrentSkipListMap[Score,String]

      val leaderboard = new ConcurrentLeaderboard(leaderboardIdentifier, new ConcurrentHashMap[MemberIdentifier,Option[Score]], new ConcurrentSkipListMap[Score,MemberIdentifier])
        idToLeaderboard.put(leaderboardIdentifier, leaderboard) match {
          case None => Success(leaderboard)
          case Some(predecessor) =>
            // TODO Technically this cannot happen because we have already tested for it
            Failure(new Exception(s"Leaderboard $leaderboardIdentifier already exists"))
        }
      case Some(leaderboard) => Failure(new Exception(s"Leaderboard $leaderboardIdentifier already exists"))
    }
  }

  def get(leaderboardIdentifier: LeaderboardIdentifier): Option[ConcurrentLeaderboard] = idToLeaderboard.get(leaderboardIdentifier)

  def get(name: String): Option[ConcurrentLeaderboard] = {
    nameToId.get(name) match {
      case None => None
      case Some(leaderboardIdentifier) => get(leaderboardIdentifier)
    }
  }

  def getInfo(name: String): Option[LeaderboardInfo] = {
    nameToId.get(name) match {
      case None => None
      case Some(leaderboardIdentifier) => getInfo(leaderboardIdentifier)
    }
  }

  def getInfo(leaderboardIdentifier: LeaderboardIdentifier): Option[LeaderboardInfo] = {
    idToLeaderboard.get(leaderboardIdentifier) match {
      case None => None
      case Some(leaderboard) => Some(leaderboard.getInfo)
    }
  }
}

/** =Leaderboard Score Keeping=
  * <p>
  * Keep track of leaderboard scores.
  * <p>
  * =Threadsafe=
  * This code is intended to be threadsafe, using concurrent non-blocking data structures, in order
  * to offer better performance over serialization in a single actor.
  *
  * @see [[net.kolotyluk.leaderboard.scorekeeping.Leaderboard]]
  * @see [[https://github.com/kolotyluk/leaderboard/blob/master/BACKGROUND.md Background]]
  * @see [[https://github.com/kolotyluk/leaderboard/blob/master/BENCHMARKS.md Background]]
  *
  * @author eric@kolotyluk.net
  */
class ConcurrentLeaderboard(override val leaderboardIdentifier: LeaderboardIdentifier, memberToScore: ConcurrentMap[MemberIdentifier,Option[Score]], scoreToMember:  ConcurrentNavigableMap[Score,MemberIdentifier])
  extends LeaderboardSync
    with Configuration
    with Logging {

  // val memberToScore = new TrieMap[String,Option[Score]]
  // val memberToScore = new ConcurrentHashMap[String,Option[Score]]
  // val scoreToMember = new ConcurrentSkipListMap[Score,String]

  // override def getInfo = LeaderboardInfo(Some(name), getCount)

  override def delete(memberIdentifier: MemberIdentifier) = {
    delete(memberIdentifier, 0)
  }

  @tailrec
  private def delete(memberIdentifier: MemberIdentifier, spinCount: Long): Boolean = {
    try {
      Metrics.checkSpinCount(memberIdentifier, spinCount)
    } catch {
      case cause: ConcurrentModificationException =>
        throw new ConcurrentModificationException(ConcurrentLeaderboard.maximumSpinCountExceeded.format("delete", memberIdentifier), cause)
    }

    // Set the spin-lock
    put(memberIdentifier, None) match {
      case None =>    // already deleted
        false
      case Some(option) => option match {
        case None =>  // Update in progress, so spin until complete
          //logger.debug(s"delete: $member locked")
          //Thread.sleep(1)
          delete(memberIdentifier, spinCount + 1)
        case Some (oldScore) => // CRITICAL SECTION
          scoreToMember.remove (oldScore)
          memberToScore.remove (memberIdentifier) // removes lock too
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
  def put(member: MemberIdentifier, score: Option[Score]) = {
    val value = memberToScore.put(member, None)
    if (value == null) None else Some(value)
  }

  def getCount = {
    memberToScore.size
  }

  override def getIdentifier = leaderboardIdentifier

  override def getInfo = LeaderboardInfo(leaderboardIdentifier, Some(name), getCount)

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

  override def getScore(memberIdentifier: MemberIdentifier): Option[Score] = {
    //if (memberIdentifier == null) logger.error(s"\n\n\n\n\nget(memberIdentifier: MemberIdentifier): memberIdentifier = $memberIdentifier\n\n\n\n\n")
    getScore(memberIdentifier, 0)
  }

  @tailrec
  final def getScore(memberIdentifier: MemberIdentifier, spinCount: Long): Option[Score] = {
    try {
      Metrics.checkSpinCount(memberIdentifier, spinCount)
    } catch {
      case cause: ConcurrentModificationException =>
        throw new ConcurrentModificationException(ConcurrentLeaderboard.maximumSpinCountExceeded.format("getScore", memberIdentifier), cause)
    }


    get(memberIdentifier) match {
      case None => None
      case Some(option) => option match {
        case None => // Update in progress, so spin until complete
          //logger.debug(s"getScore: $member locked")
          //Thread.sleep(1)
          getScore(memberIdentifier, spinCount + 1)
        case Some(score) => // CRITICAL SECTION
          Some(score)
      }
    }
  }

  def get(memberIdentifier: MemberIdentifier): Option[Option[Score]] = {
    //logger.warn(s"\n\n\n\n\nget(memberIdentifier: MemberIdentifier): memberIdentifier = $memberIdentifier\n\n\n\n\n")
    val value = memberToScore.get(memberIdentifier)
    if (value == null) None else Some(value)
  }

  override def getStanding(memberIdentifier: MemberIdentifier) = {
    getStanding(memberIdentifier, 0)
  }

  /** =Compute Standing=
    * <p>
    * Iterate through the map to compute member standing
    * <p>
    * ''Warning:'' Results may not be accurate if leaderboard is being updated while standing is being computed.
    * @param member
    * @return None if member not present, Some[Standing] otherwise
    */
  def getStanding(member: MemberIdentifier, spinCount: Long): Option[Standing] = {
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
  override def update(mode: UpdateMode, memberIdentifier: MemberIdentifier, value: BigInt) = {
    update(mode, memberIdentifier, Score(value, Random.nextLong))
  }

  override def update(mode: UpdateMode, memberIdentifier: MemberIdentifier, newScore: Score) = {
    updater(mode, memberIdentifier, newScore, 0, System.nanoTime)
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
  private def updater(mode: UpdateMode, memberIdentifier: MemberIdentifier, newScore: Score, spinCount: Int, spinStart: Long): scorekeeping.Score = {
    // Caution: there is some subtle logic below, so don't modify it unless you grok it

    try {
      Metrics.checkSpinCount(memberIdentifier, spinCount)
    } catch {
      case cause: ConcurrentModificationException =>
        throw new ConcurrentModificationException(ConcurrentLeaderboard.maximumSpinCountExceeded.format("update", memberIdentifier), cause)
    }

    // Set the spin-lock
    put(memberIdentifier, None) match {
      case None =>
        // BEGIN CRITICAL SECTION
        // Member's first time on the board
        if (scoreToMember.put(newScore, memberIdentifier) != null) {
          val message = s"$memberIdentifier: added new member in memberToScore, but found old member in scoreToMember"
          logger.error(message)
          throw new ConcurrentModificationException(message)
        }
        memberToScore.put(memberIdentifier, Some(newScore)) // remove the spin-lock
        newScore
        // END CRITICAL SECTION
      case Some(option) => option match {
        case None =>            // Update in progress, so spin until complete
          //logger.debug(s"update: $member locked, spinCount = $spinCount")
          // Waste time so the lock holder can finish, by yielding the processor to other threads
          // Exponential back-off seems to offer the best behavior
          for (i <- -1 to spinCount * spinCount) Thread.`yield`
          updater(mode, memberIdentifier, newScore, spinCount + 1, spinStart)
        case Some(oldScore) =>
          var score = newScore
          try { // BEGIN CRITICAL SECTION
            // Member already on the leaderboard
            if (scoreToMember.remove(oldScore) == null) {
              val message = s"$memberIdentifier: oldScore not found in scoreToMember, concurrency defect"
              logger.error(message)
              throw new ConcurrentModificationException(message)
            } else {
                mode match {
                  case Replace =>
                    //logger.debug(s"$member: newScore = $newScore")
                    // newScore
                  case Increment =>
                    //logger.debug(s"$member: newScore = $newScore, oldScore = $oldScore")
                    score = Score(oldScore.value + newScore.value,newScore.random )
                }
              //logger.debug(s"$member: updated score = $score")
              scoreToMember.put(score, memberIdentifier)
              memberToScore.put(memberIdentifier, Some(score))  // remove the spin-lock
              //logger.debug(s"update: $member unlocked")
            }
          } catch {
            case cause: Throwable =>
              // Unlikely to get here, but just in case, delete the member, which should also delete the lock,
              // and prevent spinning forever.
              scoreToMember.remove(oldScore)
              memberToScore.remove(memberIdentifier)
              logger.error(s"Exception in critical section. Removing member $memberIdentifier", cause)
          } // END CRITICAL SECTION
          // Do this outside the critical section to reduce time under lock
          if (spinCount > 0) Metrics.checkSpinTime(System.nanoTime() - spinStart)
          score
      }
    }
  }

}
