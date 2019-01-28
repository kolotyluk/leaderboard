package net.kolotyluk.leaderboard

import akka.http.scaladsl.server.{Route, RouteConcatenation}
import net.kolotyluk.leaderboard.Akka.swagger.SwaggerDocService
import net.kolotyluk.leaderboard.scorekeeping.{Leaderboard, LeaderboardIdentifier}

import scala.collection.concurrent.TrieMap

/** =Akka Actors=
  * ==Overview==
  * Defined behaviors for actors. In [[https://doc.akka.io/docs/akka/2.5/typed/index.html Akka Typed]],
  * a behavior is similar to the actor context in legacy Akka.
  *
  * =Guardian=
  * Is the top level actor in the Akka hierarchy of actors. That is, it the highest level actor the software developer
  * has access to. Typically started with something like:
  * {{{
  * val systemName = "Leaderboard"
  * val guardian = new Guardian() // top level of our actor hierarchy
  * val system = ActorSystem(guardian.behavior, systemName)
  * }}}
  * where [[net.kolotyluk.leaderboard.Akka.GuardianActor]] defines the behavior.
  *
  *
  */
package object Akka extends RouteConcatenation {

  val failEndpoint = new FailEndpoint
  val pingEndpoint = new PingEndpoint
  val leaderboardEndpoint = new LeaderboardEndpoint

  val routes: Route =
    failEndpoint.route ~
    pingEndpoint.route ~
    leaderboardEndpoint.routes ~
    SwaggerDocService.routes

  val leaderboardManagerActor = new LeaderboardManagerActor()

  val restActor = new RestActor(routes)
  val guardianActor = new GuardianActor(leaderboardManagerActor, restActor) // top level of our actor hierarchy

  val nameToLeaderboardIdentifier = new TrieMap[String,LeaderboardIdentifier]
  val leaderboardIdentifierToLeaderboard = new TrieMap[LeaderboardIdentifier,Leaderboard]
}

