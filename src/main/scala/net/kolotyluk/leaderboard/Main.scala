package net.kolotyluk.leaderboard

import akka.actor.typed.{ActorRef, ActorSystem, Behavior, Terminated}
import net.kolotyluk.scala.extras.{Configuration, Environment, Logging}

import scala.util.{Failure, Success}

object Main extends App
  with Configuration
  with Environment
  with Logging {

  // Safest way to indicate something is happening, don't rely on logging yet
  println("Starting " + getClass.getName)
  println("Reporting environment and configuration for troubleshooting purposes. Don't disable this.")
  printEnvironment
  printConfiguration

  logger.info("Logging started")

  val systemName = "Leaderboard"
  val system = ActorSystem(Guardian.behavior, systemName)

  implicit val executor = system.executionContext

  system.whenTerminated.onComplete {
    case Success(terminated) =>
      // println(s"Actor System Terminated Normally")
      logger.info(s"Actor System Terminated Normally")
    case Failure(cause) =>
      // println(s"Actor System Termination Failure", cause)
      logger.error(s"Actor System Termination Failure", cause)
  }

  logger.info(s"Actor System Started")

  // We're done on this thread

}
