package net.kolotyluk.leaderboard.scorekeeping

import java.util.concurrent.{ConcurrentHashMap, ConcurrentSkipListMap}

import org.scalatest.Ignore

import scala.language.postfixOps

@Ignore
class ConcurrentishLeaderboardSpec extends UnitSpec with LeaderboardBehaviors {

  behavior of "Concurrentish Leaderboard"

//  val memberToScore = new util.HashMap[String,Option[Score]]
//  val scoreToMember = new util.TreeMap[Score,String]

  val memberToScore = new ConcurrentHashMap[String,Option[Score]]
  val scoreToMember = new ConcurrentSkipListMap[Score,String]

  val leaderboard = new ConsecutiveLeaderboard(memberToScore, scoreToMember)

  it must behave like handleInitialConditionsAsync(leaderboard)
  it must behave like handleTwoMembersAsync(leaderboard)
  it must behave like handleHandleHighIntensityConcurrentUpdatesAsync(leaderboard)
  it must behave like handleHandleHighIntensityConcurrentUpdatesAsync(leaderboard)
  it must behave like handleHandleLargeNumberOfMembersAsync(leaderboard)
}
