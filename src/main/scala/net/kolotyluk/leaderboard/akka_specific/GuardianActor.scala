package net.kolotyluk.leaderboard.akka_specific

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.adapter.TypedActorSystemOps
import akka.actor.typed.{ActorRef, Behavior, PostStop, SupervisorStrategy, Terminated}
import akka.http.scaladsl.server.RouteConcatenation
import akka.util.Timeout
import net.kolotyluk.leaderboard.Configuration
import net.kolotyluk.leaderboard.akka_specific.GuardianActor._
import net.kolotyluk.leaderboard.akka_specific.RestActor.Unbind
import net.kolotyluk.scala.extras.Logging

import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.{Failure, Success}

/** =Actor Protocol=
  *
  */
object GuardianActor {
  sealed trait Message
  case class Bind() extends Message
  case class Done(cause: String) extends Message

  /** =System Shutdown Notification=
    *
    * Usually sent from the ShutdownHook to indicate the JVM is shutting down, and that the Akka system should
    * attempt a graceful shutdown, then signal when it's done.
    *
    * @param reason - why the system is shutting down
    * @param notifier - Java Object to call notifier.notify on
    */
  case class Shutdown(reason: String, notifier: Object) extends Message
  case class Spawn[M](behavior: Behavior[M], name:String) extends Message
  case class Unbound(notifier: Object, failure: Option[Throwable]) extends Message
}

/** =Outermost Behavior of ActorSystem=
  * Top level actor in Akka system, which spawns the next level of actors.
  *
  * ==Supervision==
  *
  * The main job of our Guardian Actor is to
  * [[https://doc.akka.io/docs/akka/2.5/typed/fault-tolerance.html#supervision supervise]]
  * lower level actors. In legacy Akka, if actors failed, such as from throwing an Exception, they would be
  * automatically restarted. In Akka Typed, the default action is not to restart child actors. Consequently,
  * we need to explicitly state our Supervision Policies.
  *
  * ===Initialization===
  *
  * The policy here is that if anything fails during Actor Initialization, we should consider that FATAL, and
  * shut down the system.
  *
  * ===Configuration===
  *
  * While most configuration happens during initialization, one conclusion would be that Configuration Errors
  * are also FATAL. However, configuration can happen at other times as well, such as when configuration can
  * change dynamically.
  *
  * ==Shutting Down==
  *
  * ===Bind and Unbind===
  *
  * We delegate HTTP handling to our [[RestActor]] because the Akka HTTP APIs are
  * [[https://doc.akka.io/docs/akka/current/stream Streams]]
  * based, where streams are manifested by other actors. Consequently these stream actors are child actors of
  * the RestActor, so [[https://en.wikipedia.org/wiki/Separation_of_concerns separation of concerns]] implies
  * we don't want to be concerned with those stream actors here.
  * <p>
  * When shutting down, we want to do so gracefully, such that all pending 'in-flight' HTTP requests can complete
  * normally with normal HTTP responses. We leave this up to the RestActor.
  *
  * @see [[https://doc.akka.io/docs/akka/2.5.18/typed/actors.html#introduction Akka Typed Introduction]]
  * @see [[https://doc.akka.io/docs/akka/2.5/typed/fault-tolerance.html#supervision Akka Typed Supervision]]
  */
class GuardianActor(leaderboardManagerActor: LeaderboardManagerActor, restActor: RestActor) extends RouteConcatenation with Configuration with Logging {
  logger.info("constructing...")

  val unbindTimeout = 10 seconds

  var leaderboardManagerActorRef : ActorRef[LeaderboardManagerActor.Request] = null
  var restActorRef : ActorRef[RestActor.Message] = null

  val behavior: Behavior[GuardianActor.Message] = Behaviors.setup { actorContext ⇒

    logger.info("initializing...")

    implicit val askTimeout: Timeout = 3 seconds
    implicit val actorSystem = actorContext.system.toUntyped // compatibility with legacy Akka
    implicit val executionContext = actorContext.system.executionContext
    implicit val scheduler = actorSystem.scheduler

    leaderboardManagerActorRef = actorContext.spawn(leaderboardManagerActor.behavior, "leaderboard-manager")
    assert (leaderboardManagerActorRef != null)
    Behaviors.supervise(leaderboardManagerActor.behavior)
      .onFailure[ConfigurationError](SupervisorStrategy.stop)
      .orElse(Behavior.same)
    actorContext.watch(leaderboardManagerActorRef)

    // TODO remove this for production, used for testing
    //val cancelable = actorContext.schedule(200 seconds, actorContext.self, Done("timed out"))

    Behaviors.receive[GuardianActor.Message] { (actorCell, message) ⇒
      logger.debug(s"received $message")
      message match {
        case Bind() ⇒
          try {
            restActorRef = actorCell.spawn(restActor.behavior, "REST")
            assert (restActorRef != null)
            Behaviors.supervise(restActor.behavior)
              .onFailure[ConfigurationError](SupervisorStrategy.stop)
              .orElse(Behavior.same)
            actorCell.watch(restActorRef)
            Behaviors.same
          } catch {
            case cause: AssertionError ⇒
              logger.error("Could not spawn Rest Behavior", cause)
              // TODO something better
              Behaviors.stopped
          }
        case Done(cause) ⇒
          logger.info(s"Done: $cause")
          Behaviors.same
        case Shutdown(reason, notifier) =>
          logger.warn(s"Shutting down because: $reason")
          restActorRef ! Unbind(notifier, unbindTimeout, actorContext.self)
          shutdownBehavior
        case Spawn(behavior, name) ⇒
          logger.info(s"spawning $name")
          val actorRef = actorCell.spawn(behavior, name)
          actorCell.watch(actorRef)
          Behaviors.same
        case Unbound(notifier, failure) =>
          logger.error(s"Unexpected message: $message in main behavior")
          Behaviors.same
      }
    } receiveSignal { // TODO rationalize what we really want to do here, if anything
      case (actorContext, event) ⇒
        logger.warn(s"received signal with event = $event with actorContext = $actorContext")
        event match {
          case terminated@Terminated(actorRef) ⇒
            if (actorRef == restActorRef) {
              Behavior.stopped
            } else
              Behaviors.same
//            val failure = terminated.failure
//            logger.warn(s"actorRef = $actorRef, failure = $failure")
//            failure match {
//              case None ⇒
//                logger.error(s"FATAL stopping service because of unknown failure")
//                Behaviors.stopped
//              case Some(cause) ⇒
//                if (cause.isInstanceOf[ActorInitializationException]) {
//                  if (cause.getCause.isInstanceOf[ConfigurationError]) {
//                    // Constructing a ConfigurationError logs it's own diagnostics
//                    // Terminate things so that configuration problems can be resolved first
//                    logger.error(s"FATAL - stopping service because of ConfigurationError during Actor Initialization")
//                    Behaviors.stopped
//                  } else {
//                    // Any problem during Actor Initialization is probably transient and serious enough that it is
//                    // unwise to continue with the system. TODO: reconsider this
//                    logger.error(s"FATAL - stopping service because of ActorInitializationException", cause)
//                    Behaviors.stopped
//                  }
//                } else {
//                  logger.warn(s"unknown cause = $cause, continuing...")
//                  Behaviors.same
//                }
//              case _ ⇒
//                logger.warn(s"unknown failure = $failure, continuing...")
//                Behaviors.same
//            }
          case _ ⇒
            logger.warn(s"unknown event = $event, continuing...")
            Behaviors.same
        }
      case signal@_ ⇒
        logger.warn(s"unknown signal = $signal, continuing...")
        Behaviors.same
    }
  }

  val shutdownBehavior = Behaviors.receive[GuardianActor.Message] { (actorCell, message) ⇒

    implicit val actorSystem = actorCell.system.toUntyped // compatibility with legacy Akka
    implicit val executionContext = actorCell.system.executionContext

    message match {
      case Bind() ⇒
        Behaviors.same
      case Done(cause) ⇒
        Behaviors.same
      case Shutdown(reason, replyTo) =>
        Behaviors.same
      case Spawn(behavior, name) ⇒
        Behaviors.same

      case Unbound(notifier, failure) =>

        def terminate = {
          def notifyDone = notifier.synchronized {
            logger.info("Notifying Shutdown Hook that Akka has terminated operation")
            notifier.notify
          }
          actorSystem.terminate onComplete {
            case Success(terminated) =>
              logger.info(terminated)
              notifyDone
            case Failure(cause) =>
              logger.error(cause)
              notifyDone
          }
        }

        failure match {
          case None =>
            //logger.info("REST API unbound normally after completing all requests.")
            terminate
          case Some(cause) =>
            //logger.error("REST API unbound, but may not have completed all requests", cause)
            terminate
        }

        Behaviors.same
    }
  } receiveSignal { // TODO rationalize what we really want to do here, if anything
    case (actorContext, postStop: PostStop) =>
      logger.info(postStop)
      Behaviors.same
    case (actorContext, terminated: Terminated) =>
      logger.info(terminated)
      Behaviors.same
    case signal@_ ⇒
      logger.warn(s"unknown signal = $signal, continuing...")
      Behaviors.same
  }
}
