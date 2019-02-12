package net.kolotyluk.leaderboard.Akka

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior, SupervisorStrategy, Terminated}
import akka.http.scaladsl.server.RouteConcatenation
import net.kolotyluk.leaderboard.Akka.GuardianActor.{Bind, Done, Spawn}
import net.kolotyluk.leaderboard.Configuration
import net.kolotyluk.scala.extras.Logging

object GuardianActor {
  sealed trait Message
  case class Bind() extends Message
  case class Done(cause: String) extends Message
  case class Spawn[M](behavior: Behavior[M], name:String) extends Message
}

/** =Outermost Behavior of ActorSystem=
  * Top level actor in Akka system, which spawns the next level of actors.
  * <p>
  * ==Supervision==
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
  * @see [[https://doc.akka.io/docs/akka/2.5.18/typed/actors.html#introduction Akka Typed Introduction]]
  * @see [[https://doc.akka.io/docs/akka/2.5/typed/fault-tolerance.html#supervision Akka Typed Supervision]]
  */
class GuardianActor(leaderboardManagerActor: LeaderboardManagerActor, restActor: RestActor) extends RouteConcatenation with Configuration with Logging {
  logger.info("constructing...")

  var leaderboardManagerActorRef : ActorRef[LeaderboardManagerActor.Request] = null
  var restActorRef : ActorRef[RestActor.Message] = null

  val behavior: Behavior[GuardianActor.Message] = Behaviors.setup { actorContext ⇒

    logger.info("initializing...")

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

            leaderboardManagerActorRef = actorCell.spawn(leaderboardManagerActor.behavior, "leaderboard-manager")
            assert (leaderboardManagerActorRef != null)
            Behaviors.supervise(leaderboardManagerActor.behavior)
              .onFailure[ConfigurationError](SupervisorStrategy.stop)
              .orElse(Behavior.same)
            actorCell.watch(leaderboardManagerActorRef)

            Behaviors.same
          } catch {
            case cause: AssertionError ⇒
              logger.error("Could not spawn Rest Behavior", cause)
              // TODO something better
              Behaviors.stopped
          }
        case Done(cause) ⇒
          logger.info(s"Done: $cause")
          Behaviors.stopped
        case Spawn(behavior, name) ⇒
          logger.info(s"spawning $name")
          val actorRef = actorCell.spawn(behavior, name)
          actorCell.watch(actorRef)
          //actorRef ! Start()
          Behaviors.same
      }
    } receiveSignal {
      case (actorContext, event) ⇒
        logger.warn(s"received signal with event = $event with actorContext = $actorContext")
        event match {
          case terminated@Terminated(actorRef) ⇒
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
}
