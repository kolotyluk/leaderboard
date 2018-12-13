package net.kolotyluk.leaderboard.scorekeeping

import java.util

import scala.language.postfixOps

class ConsecutiveLeaderboardSpec extends UnitSpec with LeaderboardBehaviors {

  behavior of "Consecutive Leaderboard"

  val memberToScore = new util.HashMap[String,Option[Score]]
  val scoreToMember = new util.TreeMap[Score,String]

  val leaderboard = new ConsecutiveLeaderboard(memberToScore, scoreToMember)

  it must behave like handleInitialConditions(leaderboard)
  it must behave like handleTwoMembers(leaderboard)
  it must behave like handleHandleConcurrentUpdates(leaderboard)
  it must behave like handleHandleHighIntensityConcurrentUpdates(leaderboard)
  it must behave like handleHandleLargeNumberOfMembers(leaderboard)
}
