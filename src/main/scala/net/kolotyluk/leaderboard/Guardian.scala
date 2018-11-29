package net.kolotyluk.leaderboard

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{Behavior, Terminated}
import net.kolotyluk.leaderboard.service.REST
import net.kolotyluk.scala.extras.Logging


import scala.concurrent.duration._

/** =Outermost Behavior of ActorSystem=
  * <p>
  * Top level actor in Akka system, which spawns the next level of actors
  *
  * https://doc.akka.io/docs/akka/2.5.18/typed/actors.html#introduction
  */
object Guardian extends Logging {
  logger.info("initializing...")

  sealed trait Message
  case class Done(cause: String) extends Message
  case class Spawn[M](behavior: Behavior[M], name:String) extends Message

  val behavior: Behavior[Message] = Behaviors.setup { actorContext ⇒
    logger.info(s"starting $actorContext")

    // TODO remove this for production, used for testing
    //val cancelable = actorContext.schedule(200 seconds, actorContext.self, Done("timed out"))

    // val http = actorContext.spawn(Http.behavior, "Http")
    // actorContext.watch(http)

    Behaviors.receive[Message] { (actorCell, message) ⇒
      logger.info(s"received $message")
      message match {
        case Done(cause) =>
          logger.info(s"Done: $cause")
          Behaviors.stopped
        case Spawn(behavior, name) =>
          logger.info(s"spawning $name")
          val actorRef = actorCell.spawn(behavior, name)
          actorCell.watch(actorRef)
          //actorRef ! Start()
          Behaviors.same
      }
    } receiveSignal {
      case (actorContext, Terminated(actorRef)) ⇒
        // There is no other information available with this signal.
        // While akka knows the reason for termination, we don't.
        logger.info(s"actorContext = $actorContext")
        logger.info(s"actorRef = $actorRef")
        Behaviors.same
    }
  }
}
