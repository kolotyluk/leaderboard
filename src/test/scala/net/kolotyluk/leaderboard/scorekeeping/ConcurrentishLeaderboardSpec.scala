package net.kolotyluk.leaderboard.scorekeeping

import java.util.UUID
import java.util.concurrent.{ConcurrentHashMap, ConcurrentSkipListMap}

import net.kolotyluk.scala.extras.Internalized
import org.scalatest.Ignore
import unit.UnitSpec

import scala.language.postfixOps

@Ignore
class ConcurrentishLeaderboardSpec extends UnitSpec with LeaderboardBehaviors {

  behavior of "Concurrentish Leaderboard"

  val leaderboardIdentifier = Internalized(UUID.randomUUID())
  val memberToScore = new ConcurrentHashMap[MemberIdentifier,Option[Score]]
  val scoreToMember = new ConcurrentSkipListMap[Score,MemberIdentifier]

  val leaderboard = new ConsecutiveLeaderboard(leaderboardIdentifier, memberToScore, scoreToMember)

  it must behave like handleInitialConditionsAsync(leaderboard)
  it must behave like handleTwoMembersAsync(leaderboard)
  it must behave like handleConcurrentUpdatesAsync(leaderboard)
  it must behave like handleHandleHighIntensityConcurrentUpdatesAsync(leaderboard)
  it must behave like handleHandleLargeNumberOfMembersAsync(leaderboard)
}
