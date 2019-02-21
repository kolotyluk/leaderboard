package net.kolotyluk.leaderboard

import akka.http.scaladsl.server.RouteConcatenation

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
  * where [[net.kolotyluk.leaderboard.akka_specific.GuardianActor]] defines the behavior.
  *
  *
  */
package object akka_specific extends RouteConcatenation {
  val leaderboardManagerActor = new LeaderboardManagerActor()
  val restActor = new RestActor(endpoint.routes)
  val guardianActor = new GuardianActor(leaderboardManagerActor, restActor) // top level of our actor hierarchy
}

