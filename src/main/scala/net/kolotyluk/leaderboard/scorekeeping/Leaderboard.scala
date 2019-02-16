package net.kolotyluk.leaderboard.scorekeeping

import scala.language.higherKinds

/** =Leaderboard Interface=
  *
  * Fundamental API for all Leaderboard access.
  *
  * There are both synchronous and asynchronous versions of this API/
  *
  * @see [[net.kolotyluk.leaderboard.scorekeeping.LeaderboardAsync]]
  * @see [[net.kolotyluk.leaderboard.scorekeeping.LeaderboardSync]]
  *
  */
trait Leaderboard {

  /** =Abstract Result Type=
    * This API supports both asynchronous and synchronous responses.
    * ==Examples==
    * {{{
    * leaderboard.getCount match {
    *   case future: Future[Int]] =>
    *     future.map(count => LeaderboardStatusResponse(leaderboardUrlId, count))
    *   case count: Int =>
    *     Future.successful(LeaderboardStatusResponse(leaderboardUrlId, count)
    * }
    * }}}
    * or more concisely
    * {{{
    * getFutureResult[Int,LeaderboardStatusResponse](
    *   leaderboard.getCount,
    *   count =>LeaderboardStatusResponse(leaderboardId, count) )
    * }}}
    *
    * @tparam A either Future[Type] or Type
    */
  type AbstractResult[A] // Needs to be define here, and not at some other level

  val leaderboardIdentifier: LeaderboardIdentifier = null
  var name: String = null

  /** =Delete Member=
    * Delete member from leaderboard
    * @param memberIdentifier
    * @return true, if member was on leaderboard
    */
  def delete(memberIdentifier: MemberIdentifier): AbstractResult[Boolean]

  /** =Leaderboard Member Count=
    * Get the total count of all members on the leaderboard
    * @return count
    */
  def getCount: AbstractResult[Int]

  def getIdentifier: AbstractResult[LeaderboardIdentifier]

  def getInfo: AbstractResult[LeaderboardInfo]

  def getName: AbstractResult[Option[String]]

  /** =Get Range of Scores=
    * <p>
    * Return a range of scores from start to stop
    * <p>
    *
    * @param start
    * @param stop
    * @return
    * @throws IndexOutOfBoundsException if more than Int.MaxValue scores in the result
    *
    * @see [[https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/IndexOutOfBoundsException.html java.lang.IndexOutOfBoundsException]]
    */
  def getRange(start: Long, stop: Long): AbstractResult[Range]

  /** =Member's Score=
    * Get the member's score from the leaderboard
    *
    * @param memberIdentifier
    * @return score
    */
  def getScore(memberIdentifier: MemberIdentifier): AbstractResult[Option[Score]]

  /** =Compute Standing=
    * <p>
    * Iterate through the map to compute member standing
    *
    * ==Performance==
    * O(N) where N = count of members on leaderboard
    *
    * @param memberIdentifier
    * @return None if member not present, Some[Standing] otherwise
    */
  def getStanding(memberIdentifier: MemberIdentifier): AbstractResult[Option[Standing]]

  def update(mode: UpdateMode, memberIdentifier: MemberIdentifier, value: BigInt): AbstractResult[Score]

  /** =Update Member Score=
    * Update member's score on leaderboard
    * <p>
    * This should only be called from another ScoreKeeper, running in a different Akka Cluster Node.
    * <p>
    * Performance is a little better for Replace than Increment, but for both cases, performance is O(log n).
    * <p>
    * ==Performance==
    * O(log N) where N = count of members on the leaderboard
    *
    * @param memberIdentfier   member ID
    * @param newScore existing score created by another ScoreKeeper
    */
  def update(mode: UpdateMode, memberIdentifier: MemberIdentifier, newScore: Score): AbstractResult[Score]

}
