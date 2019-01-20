package net.kolotyluk.leaderboard.scorekeeping

import java.util.concurrent.{ConcurrentHashMap, ConcurrentSkipListMap}

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import net.kolotyluk.leaderboard.Akka.LeaderboardActor
import org.scalatest.BeforeAndAfterAll

import scala.language.postfixOps

class LeaderboardActorSpec extends UnitSpec with LeaderboardBehaviors with BeforeAndAfterAll {

  val testKit = ActorTestKit()

  override def afterAll(): Unit = testKit.shutdownTestKit()

  behavior of "Leaderboard Actor"

//  val memberToScore = new util.HashMap[String,Option[Score]]
//  val scoreToMember = new util.TreeMap[Score,String]

  val memberToScore = new ConcurrentHashMap[String,Option[Score]]
  val scoreToMember = new ConcurrentSkipListMap[Score,String]

  val leaderboard = new ConsecutiveLeaderboard(memberToScore, scoreToMember)
  val leaderboardActor = new LeaderboardActor(leaderboard)

  val actor = testKit.spawn(leaderboardActor.behavior, "leaderboard")

  it must behave like handleInitialConditionsAsync(leaderboardActor)
  it must behave like handleTwoMembersAsync(leaderboardActor)
  it must behave like handleConcurrentUpdatesAsync(leaderboardActor)
  it must behave like handleHandleHighIntensityConcurrentUpdatesAsync(leaderboardActor)
  it must behave like handleHandleLargeNumberOfMembersAsync(leaderboardActor)
}
