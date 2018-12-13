package net.kolotyluk.leaderboard.scorekeeping

import scala.language.postfixOps
import scala.util.{Failure, Success}

class ConcurrentLeaderboardSpec extends UnitSpec with LeaderboardBehaviors {

  behavior of "Concurrent Leaderboard"

  val leaderboard = ConcurrentLeaderboard.add match {
    case Failure(cause) => throw cause
    case Success(leaderboard) => leaderboard
  }

  it must behave like handleInitialConditions(leaderboard)
  it must behave like handleTwoMembers(leaderboard)
  it must behave like handleHandleConcurrentUpdates(leaderboard)
  it must behave like handleHandleHighIntensityConcurrentUpdates(leaderboard)
  it must behave like handleHandleLargeNumberOfMembers(leaderboard)
}
