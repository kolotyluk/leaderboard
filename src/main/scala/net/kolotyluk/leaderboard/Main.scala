package net.kolotyluk.leaderboard

import akka.actor.typed.ActorSystem
import net.kolotyluk.leaderboard.behavior.guardian
import net.kolotyluk.leaderboard.behavior.guardian.Bind
import net.kolotyluk.scala.extras.{Configuration, Environment, Logging}

import scala.util.{Failure, Success}

object Main
  extends App
    with Configuration
    with Environment
    with Logging {

  // Safest way to indicate something is happening, don't rely on logging yet
  println(s"Starting ${getClass.getName}...")
  println("Reporting environment and configuration for troubleshooting purposes. Don't disable this.")
  printEnvironment
  printConfiguration

  logger.info("Logging started")

  // Start the Akka actor system, with the top level guardian actor, using its default behavior
  val system = ActorSystem(guardian.behavior, config.getAkkaSystemName(), com.typesafe.config.ConfigFactory.load)

  logger.info(s"Akka Actor System Started")

  system ! (Bind()) // to our HTTP REST endpoint

  system.whenTerminated.onComplete {
    case Success(terminated) =>
      // println(s"Actor System Terminated Normally")
      logger.info(s"Actor System Terminated Normally")
    case Failure(cause) =>
      // println(s"Actor System Termination Failure", cause)
      logger.error(s"Actor System Termination Failure", cause)
  } (system.executionContext)

  // We're done on this thread. the Akka system is running the show now
}
