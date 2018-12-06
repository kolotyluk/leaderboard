package net.kolotyluk.leaderboard.actor

import akka.actor.ActorInitializationException
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior, SupervisorStrategy, Terminated}
import akka.http.scaladsl.server.RouteConcatenation
import net.kolotyluk.leaderboard.Configuration
import net.kolotyluk.leaderboard.actor.Guardian.{Bind, Done, Spawn}
import net.kolotyluk.scala.extras.Logging

object Guardian {
  sealed trait Message
  case class Bind() extends Message
  case class Done(cause: String) extends Message
  case class Spawn[M](behavior: Behavior[M], name:String) extends Message
}

/** =Outermost Behavior of ActorSystem=
  * <p>
  * Top level actor in Akka system, which spawns the next level of actors
  *
  * https://doc.akka.io/docs/akka/2.5.18/typed/actors.html#introduction
  *
  * https://doc.akka.io/docs/akka/2.5/typed/fault-tolerance.html#supervision
  */
class Guardian(restApi: Rest) extends RouteConcatenation with Configuration with Logging {
  logger.info("actor initializing...")

  var restActorRef : ActorRef[Rest.Message] = null

  val behavior: Behavior[Guardian.Message] = Behaviors.setup { actorContext ⇒

    logger.info("setting up...")

    // TODO remove this for production, used for testing
    //val cancelable = actorContext.schedule(200 seconds, actorContext.self, Done("timed out"))

    Behaviors.receive[Guardian.Message] { (actorCell, message) ⇒
      logger.debug(s"received $message")
      message match {
        case Bind() ⇒
          try {
            restActorRef = actorCell.spawn(restApi.behavior, "REST")
            assert (restActorRef != null)
            Behaviors.supervise(restApi.behavior)
              .onFailure[ConfigurationException](SupervisorStrategy.stop)
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
            val failure = terminated.failure
            logger.warn(s"actorRef = $actorRef, failure = $failure")
            failure match {
              case None ⇒
                Behaviors.stopped
              case Some(ConfigurationException(path,value,message,cause)) ⇒
                logger.error(s"FATAL stopping service because of configuration errors")
                Behaviors.stopped
              case Some(cause) ⇒
                if (cause.isInstanceOf[ActorInitializationException]) {
                  logger.error(s"FATAL stopping service because of ActorInitializationException")
                  if (cause.getCause.isInstanceOf[ConfigurationException]) logger.error(s"FATAL stopping service because of ConfigurationException")
                  Behaviors.stopped
                } else {
                  logger.warn(s"unknown cause = $cause, continuing...")
                  Behaviors.same
                }
              case _ ⇒
                logger.warn(s"unknown failure = $failure, continuing...")
                Behaviors.same
            }
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
