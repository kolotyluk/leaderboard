package net.kolotyluk.leaderboard.scorekeeping

import java.util
import java.util.UUID

import net.kolotyluk.scala.extras.Internalized
import unit.UnitSpec

import scala.language.postfixOps

class SynchronizedLeaderboardSpec extends UnitSpec  with LeaderboardBehaviors {

  behavior of "Synchronized Leaderboard"

  val leaderboardIdentifier = Internalized(UUID.randomUUID())
  val memberToScore = new util.HashMap[MemberIdentifier, Option[Score]]
  val scoreToMember = new util.TreeMap[Score, MemberIdentifier]

  val leaderboard = new SynchronizedLeaderboard(leaderboardIdentifier, memberToScore, scoreToMember)

  it must behave like handleInitialConditionsAsync(leaderboard)
  it must behave like handleTwoMembersAsync(leaderboard)
  it must behave like handleConcurrentUpdatesAsync(leaderboard)
  it must behave like handleHandleHighIntensityConcurrentUpdatesAsync(leaderboard)
  it must behave like handleHandleLargeNumberOfMembersAsync(leaderboard)
}
