package net.kolotyluk.leaderboard.behavior

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior, Terminated}
import akka.http.scaladsl.server.RouteConcatenation
import net.kolotyluk.scala.extras.Logging

/** =Outermost Behavior of ActorSystem=
  * <p>
  * Top level actor in Akka system, which spawns the next level of actors
  *
  * https://doc.akka.io/docs/akka/2.5.18/typed/actors.html#introduction
  */
class Guardian extends RouteConcatenation with Logging {
  logger.info("actor initializing...")

  sealed trait Message
  case class Bind() extends Message
  case class Done(cause: String) extends Message
  case class Spawn[M](behavior: Behavior[M], name:String) extends Message

  val restApi = new Rest()
  var restActorRefOption : Option[ActorRef[restApi.Message]] = None
  var restActorRef : ActorRef[restApi.Message] = null

  val behavior: Behavior[Message] = Behaviors.setup { actorContext ⇒
    logger.info("setting up...")

    // TODO remove this for production, used for testing
    //val cancelable = actorContext.schedule(200 seconds, actorContext.self, Done("timed out"))

    // val http = actorContext.spawn(Http.behavior, "Http")
    // actorContext.watch(http)

    Behaviors.receive[Message] { (actorCell, message) ⇒
      logger.info(s"received $message")
      message match {
        case Bind() ⇒
          try {
            restActorRef = actorCell.spawn(restApi.behavior, "REST")
            assert (restActorRef != null)
            actorCell.watch(restActorRef)
            restActorRefOption = Some(restActorRef)
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
      case (actorContext, Terminated(actorRef)) ⇒
        // There is no other information available with this signal.
        // While akka knows the reason for termination, we don't.
        logger.info(s"actorContext = $actorContext")
        logger.info(s"actorRef = $actorRef")
        actorRef match {
          case restActorRef ⇒
          // TODO should we rebind here?
            logger.error("rest actor terminated")
        }
        Behaviors.same
    }
  }
}
