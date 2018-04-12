package net.kolotyluk.leaderboard

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{Behavior, Terminated}
import net.kolotyluk.leaderboard.service.REST
import net.kolotyluk.scala.extras.Logging


import scala.concurrent.duration._

/** =Outermost Behavior of ActorSystem=
  * <p>
  * Top level actor in Akka system, which spawns the next level of actors
  */
object Guardian extends Logging {
  logger.info("initializing...")

  sealed trait Message
  case class Done(cause: String) extends Message
  case class Spawn[M](behavior: Behavior[M], name:String) extends Message

  val behavior: Behavior[Message] = Behaviors.setup {
    actorContext ⇒
      logger.info("starting...")

      // TODO remove this for production, used for testing
      //val cancelable = actorContext.schedule(200 seconds, actorContext.self, Done("timed out"))

      //val http = actorContext.spawn(Http.behavior, "Http")
      //actorContext.watch(http)

      // TODO should we convert this to a simple function instead of a message?
      actorContext.self ! Spawn(REST.behavior, "Http")

      Behaviors.immutable[Message] { (actorCell, message) ⇒
        logger.info(s"received $message")
        message match {
          case Done(cause) =>
            Behaviors.stopped
          case Spawn(behavior, name) ⇒
            logger.info(s"spawning $name")
            val actorRef = actorCell.spawn(behavior, name)
            actorCell.watch(actorRef)
            //actorRef ! Start()
            Behaviors.same
        }
      } onSignal {
        // There is no other information available with this signal.
        // While akka knows the reason for termination, we don't.
        case (actorCell, Terminated(actorRef)) ⇒
          logger.warn(s"Terminated ${actorRef.path.name}")
          Behaviors.same
      }
  }
}
