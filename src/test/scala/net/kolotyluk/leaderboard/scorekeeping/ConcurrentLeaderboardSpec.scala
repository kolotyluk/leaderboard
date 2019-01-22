package net.kolotyluk.leaderboard.scorekeeping

import java.util.concurrent.{ConcurrentHashMap, ConcurrentSkipListMap}

import unit.UnitSpec

import scala.language.postfixOps

class ConcurrentLeaderboardSpec extends UnitSpec with LeaderboardBehaviors {

  behavior of "Concurrent Leaderboard"

//  val leaderboard = ConcurrentLeaderboard.add match {
//    case Failure(cause) => throw cause
//    case Success(leaderboard) => leaderboard
//  }

  // val memberToScore2 = new TrieMap[String,Option[Score]]
  val memberToScore = new ConcurrentHashMap[String,Option[Score]]
  val scoreToMember = new ConcurrentSkipListMap[Score,String]

  val leaderboard = new ConcurrentLeaderboard(memberToScore, scoreToMember)

  it must behave like handleInitialConditionsAsync(leaderboard)
  it must behave like handleTwoMembersAsync(leaderboard)
  it must behave like handleConcurrentUpdatesAsync(leaderboard)
  it must behave like handleHandleHighIntensityConcurrentUpdatesAsync(leaderboard)
  it must behave like handleHandleLargeNumberOfMembersAsync(leaderboard)
}
