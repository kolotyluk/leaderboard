package net.kolotyluk.leaderboard.Akka.endpoint

import java.util.UUID

import net.kolotyluk.akka.scala.extras.base64uuid
import net.kolotyluk.leaderboard.InternalIdentifier
import net.kolotyluk.leaderboard.scorekeeping.{Leaderboard, LeaderboardIdentifier}
import net.kolotyluk.scala.extras.Internalized

import scala.collection.concurrent.TrieMap

import akka.http.scaladsl.server.PathMatcher1

package object leaderboard {

  val nameToLeaderboardIdentifier = new TrieMap[String,LeaderboardIdentifier]
  val identifierToLeaderboard = new TrieMap[LeaderboardIdentifier,Leaderboard]

  val leaderboardEndpoint = new LeaderboardEndpoint

  // https://doc.akka.io/docs/akka-http/current/routing-dsl/path-matchers.html
  // https://github.com/spray/spray/blob/master/spray-routing/src/main/scala/spray/routing/PathMatcher.scala
  val base64identifier: PathMatcher1[InternalIdentifier[UUID]] = base64uuid flatMap { uuid => Some(Internalized(uuid)) }

}
