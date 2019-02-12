package net.kolotyluk.leaderboard.scorekeeping

import scala.language.higherKinds


/** =Leaderboard Interface=
  *
  * Fundamental API for all Leaderboard access.
  *
  *
  */
trait Leaderboard {

  /** =Abstract Response Type=
    * This API supports both asynchronous and synchronous responses.
    * ==Examples==
    * {{{
    * val count = leaderboard.getCount
    * if (count.isInstanceOf[Int]) {
    *   Future.successful(LeaderboardStatusResponse(leaderboardUrlId, count.asInstanceOf[Int]))
    * } else {
    *   count.asInstanceOf[Future[Int]].map{ futureCount =>
    *     LeaderboardStatusResponse(leaderboardUrlId, futureCount)
    *   }
    * }
    * }}}
    *
    * @tparam A either Future[A] or A
    */
  type Response[A]

  val leaderboardIdentifier: LeaderboardIdentifier = null
  var name: String = null

  /** =Delete Member=
    * Delete member from leaderboard
    * @param memberIdentifier
    * @return true, if member was on leaderboard
    */
  def delete(memberIdentifier: MemberIdentifier): Response[Boolean]

  /** =Leaderboard Member Count=
    * Get the total count of all members on the leaderboard
    * @return count
    */
  def getCount: Response[Int]

  def getIdentifier: Response[LeaderboardIdentifier]

  def getInfo: Response[LeaderboardInfo]

  def getName: Response[Option[String]]

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
  def getRange(start: Long, stop: Long): Response[Range]

  /** =Member's Score=
    * Get the member's score from the leaderboard
    *
    * @param memberIdentifier
    * @return score
    */
  def getScore(memberIdentifier: MemberIdentifier): Response[Option[BigInt]]

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
  def getStanding(memberIdentifier: MemberIdentifier): Response[Option[Standing]]

  def update(mode: UpdateMode, memberIdentifier: MemberIdentifier, value: BigInt): Response[Score]

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
  def update(mode: UpdateMode, memberIdentifier: MemberIdentifier, newScore: Score): Response[Score]

}
