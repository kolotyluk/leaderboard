package net.kolotyluk.leaderboard.scorekeeping

import java.util

import scala.language.postfixOps

class SynchronizedLeaderboardSpec extends UnitSpec  with LeaderboardBehaviors {

  behavior of "Synchronized Leaderboard"

  val memberToScore = new util.HashMap[String, Option[Score]]
  val scoreToMember = new util.TreeMap[Score, String]

  val leaderboard = new SynchronizedLeaderboard(memberToScore, scoreToMember)

  it must behave like handleInitialConditionsAsync(leaderboard)
  it must behave like handleTwoMembersAsync(leaderboard)
  it must behave like handleConcurrentUpdatesAsync(leaderboard)
  it must behave like handleHandleHighIntensityConcurrentUpdatesAsync(leaderboard)
  it must behave like handleHandleLargeNumberOfMembersAsync(leaderboard)
}
