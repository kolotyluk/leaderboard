package net.kolotyluk.leaderboard

import akka.actor.typed.{ActorSystem, Terminated}
import net.kolotyluk.leaderboard.akka_specific.GuardianActor.{Bind, Shutdown}
import net.kolotyluk.leaderboard.akka_specific.guardianActor
import net.kolotyluk.scala.extras.{Environment, Logging}

import scala.language.postfixOps
import scala.util.{Failure, Success}


/** =Leaderboard Micro Service - Main Entry Point=
  *
  * ==Startup==
  *
  * Akka System Startup - set up our operating context with configuration, environment, and logging before
  * starting the actor system.
  *
  * ==Shutdown==
  *
  * While all systems should be designed to be robust when things spontaneously fail, with HTTP APIs, it's nice to
  * make provisions for an orderly shutdown so that any outstanding HTTP Requests can complete normally.
  * <p>
  * Note: normal shutdown will not happen if you press the IntelliJ Stop Button. Instead, you need to use the `Exit`
  * button in the Run panel. This will only work when Running, and not when Debugging.
  *
  * ===Shutdown Hook===
  *
  * By registering a Shutdown Hook, an application can perform any necessary housekeeping process before the JVM
  * shuts down. A Shutdown Hook is a Java Thread, and when all such threads complete, the JVM will finally complete
  * the shutdown process.
  * <p>
  * By using the Shutdown Hook, this service can make best effort to shutdown gracefully, minimizing loss of data
  * or operations. Note, because the hook runs at a rather low lever, we utilize fairly primitive mechanisms for
  * the Akka System to notify the Shutdown Hook when it's done.
  * <p>
  * The easist way to shutdown the service is
  * {{{
  * System.exit(0)
  * }}}
  *
  * @author eric@kolotyluk.net
  * @see [[https://doc.akka.io/docs/akka/current Akka]]
  * @see [[https://doc.akka.io/docs/akka/current/typed/index.html#akka-typed Akka Typed]]
  * @see [[http://stackoverflow.com/questions/4727536/how-do-i-stop-a-processing-running-in-intellij-such-that-it-calls-the-shutdown-h ItelliJ Exit Button]]
  * @see [[https://docs.oracle.com/javase/8/docs/technotes/guides/lang/hook-design.html Design of the Shutdown Hooks API]]
  * @see [[https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/Runtime.html#addShutdownHook(java.lang.Thread) addShutdownHook]]
  */
object Main
  extends App
    with Configuration
    with Environment
    with Logging {

  val pid = ProcessHandle.current.pid

  // Safest way to indicate something is happening, don't rely on logging yet
  println(s"Process $pid, starting ${getClass.getName}...")
  println("Reporting environment and configuration for troubleshooting purposes. Don't disable this.")

  println(environment.getEnvironmentReport())
  println(config.getConfigurationReport())

  logger.info("Logging started")

  // Start the Akka actor system, with the top level guardian actor, using its default behavior
  val system = ActorSystem(guardianActor.behavior, config.getAkkaSystemName())

  logger.info(s"Akka Actor System Started")

  system ! Bind() // to our HTTP REST endpoint

  system.whenTerminated.onComplete {
    case Success(Terminated(actorRef)) =>
      // println(s"Actor System Terminated Normally")
      logger.info(s"Actor System Terminated Normally")
    case Failure(cause) =>
      // println(s"Actor System Termination Failure", cause)
      logger.error(s"Actor System Termination Failure", cause)
  } (system.executionContext)

  Runtime.getRuntime.addShutdownHook(new Thread {
    override def run() = {
      println(s"Process $pid terminating")
      val notifier = new Object

      system ! Shutdown("Shutdown Hook Called", notifier)

      // Block this thread until Akka is done, and then the JVM will finish shutting down.
      notifier.synchronized {
        try {
          notifier.wait(10000)
          logger.info("Successful Shutdown")
        } catch {
          case cause: Throwable =>
            logger.error(cause)
        }
      }
    }
  })

  // We're done on this thread. the Akka system is running the show now
}
