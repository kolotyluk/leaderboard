package net.kolotyluk.leaderboard.scorekeeping

import java.util.concurrent.{ConcurrentHashMap, ConcurrentSkipListMap}

import scala.language.postfixOps

class SynchronizedConcurrentLeaderboardSpec extends UnitSpec  with LeaderboardBehaviors {

  behavior of "Synchronized Concurrent Leaderboard"

  val memberToScore = new ConcurrentHashMap[String, Option[Score]]
  val scoreToMember = new ConcurrentSkipListMap[Score, String]

  val leaderboard = new SynchronizedConcurrentLeaderboard(memberToScore, scoreToMember)

  it must behave like handleInitialConditionsAsync(leaderboard)
  it must behave like handleTwoMembersAsync(leaderboard)
  it must behave like handleConcurrentUpdatesAsync(leaderboard)
  it must behave like handleHandleHighIntensityConcurrentUpdatesAsync(leaderboard)
  it must behave like handleHandleLargeNumberOfMembersAsync(leaderboard)
}
