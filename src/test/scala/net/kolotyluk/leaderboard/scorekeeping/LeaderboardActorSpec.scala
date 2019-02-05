package net.kolotyluk.leaderboard.scorekeeping

import java.util
import java.util.UUID

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import net.kolotyluk.leaderboard.Akka.LeaderboardActor
import net.kolotyluk.scala.extras.Internalized
import org.scalatest.BeforeAndAfterAll
import unit.UnitSpec

import scala.language.postfixOps

class LeaderboardActorSpec extends UnitSpec with LeaderboardBehaviors with BeforeAndAfterAll {

  val testKit = ActorTestKit()

  override def afterAll(): Unit = testKit.shutdownTestKit()

  behavior of "Leaderboard Actor"

  val leaderboardIdentifier = Internalized(UUID.randomUUID())
  val memberToScore = new util.HashMap[MemberIdentifier,Option[Score]]
  val scoreToMember = new util.TreeMap[Score,MemberIdentifier]

  val leaderboard = new ConsecutiveLeaderboard(leaderboardIdentifier, memberToScore, scoreToMember)
  val leaderboardActor = new LeaderboardActor(leaderboardIdentifier, leaderboard)

  val actor = testKit.spawn(leaderboardActor.behavior, "leaderboard")

  it must behave like handleInitialConditionsAsync(leaderboardActor)
  it must behave like handleTwoMembersAsync(leaderboardActor)
  it must behave like handleConcurrentUpdatesAsync(leaderboardActor)
  it must behave like handleHandleHighIntensityConcurrentUpdatesAsync(leaderboardActor)
  it must behave like handleHandleLargeNumberOfMembersAsync(leaderboardActor)
}
