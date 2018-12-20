package net.kolotyluk.leaderboard.scorekeeping

import java.util.UUID

import scala.language.higherKinds

trait Leaderboard {
  type Response[A]

  var uuid: UUID = null
  var name: String = null

  /** =Delete Member
    * Delete member from leaderboard
    * @param member
    * @return true, if member was on leaderboard
    */
  def delete(member: String): Response[Boolean]

  /** =Leaderboard Member Count=
    * Get the total count of all members on the leaderboard
    * @return count
    */
  def getCount: Response[Int]

  def getInfo: Response[Info]

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
    */
  def getRange(start: Long, stop: Long): Response[Range]

  /** =Member's Score=
    * Get the member's score from the leaderboard
    *
    * @param member
    * @return score
    */
  def getScore(member: String): Response[Option[BigInt]]

  /** =Compute Standing=
    * <p>
    * Iterate through the map to compute member standing
    *
    * ==Performance==
    * O(N) where N = count of members on leaderboard
    *
    * @param member
    * @return None if member not present, Some[Standing] otherwise
    */
  def getStanding(member: String): Response[Option[Standing]]

  def getUrlIdentifier(identifier: String): Response[UUID]

  def getUrlIdentifier(uuid: UUID = UUID.randomUUID()): Response[String]

  def getUuid: Response[UUID]

  /** =Update Member Score=
    * Update member's score on leaderboard
    */
  def update(mode: UpdateMode, member: String, value: BigInt): Response[Score]

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
    * @param member   member ID
    * @param newScore existing score created by another ScoreKeeper
    */
  def update(mode: UpdateMode, member: String, newScore: Score): Response[Score]

}
