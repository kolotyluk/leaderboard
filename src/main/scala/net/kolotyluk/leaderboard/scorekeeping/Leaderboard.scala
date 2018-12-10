package net.kolotyluk.leaderboard.scorekeeping

import java.util.UUID

trait Leaderboard {

  /** =Delete Member
    * Delete member from leaderboard
    * @param member
    * @return true, if member was on leaderboard
    */
  def delete(member: String): Boolean

  /** =Leaderboard Member Count=
    * Get the total count of all members on the leaderboard
    * @return count
    */
  def getCount: Int

  def getInfo: Info

  def getName: Option[String]

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
  def getRange(start: Long, stop: Long): Range

  /** =Member's Score=
    * Get the member's score from the leaderboard
    *
    * @param member
    * @return score
    */
  def getScore(member: String): Option[BigInt]

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
  def getStanding(member: String): Option[Standing]

  def getUrlIdentifier(identifier: String): UUID

  def getUrlIdentifier(uuid: UUID = UUID.randomUUID()): String

  def getUuid: UUID

  def setName(name: Option[String])

  /** =Update Member Score=
    * Update member's score on leaderboard
    */
  def update(mode: UpdateMode, member: String, value: BigInt)

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
  def update(mode: UpdateMode, member: String, newScore: Score)

}
