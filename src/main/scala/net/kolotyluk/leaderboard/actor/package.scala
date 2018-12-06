package net.kolotyluk.leaderboard

/** =Akka Actor Behaviors=
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
  * where [[net.kolotyluk.leaderboard.actor.Guardian]] defines the behavior.
  *
  *
  */
package object actor {
  val rest = new Rest()
  val guardian = new Guardian(rest) // top level of our actor hierarchy
}
