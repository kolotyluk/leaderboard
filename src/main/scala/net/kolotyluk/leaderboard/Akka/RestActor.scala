package net.kolotyluk.leaderboard.Akka

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.adapter.TypedActorSystemOps
import akka.actor.typed.{ActorRef, Behavior, Terminated}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import net.kolotyluk.leaderboard.Configuration
import net.kolotyluk.leaderboard.Akka.RestActor.{Done, Fail, Message, Spawn}
import net.kolotyluk.scala.extras.Logging

import scala.util.{Failure, Success}


object RestActor {
  sealed trait Message
  case class Done(cause: String) extends Message
  case class Fail(cause: Throwable) extends Message
  case class Spawn[M](behavior: Behavior[M], name:String) extends Message
}

/** =HTTP REST API=
  * Binds to one or more interfaces listening for HTTP Requests
  *
  * ==Akka HTTP==
  * Binds using [[https://doc.akka.io/docs/akka-http/current/server-side/index.html Akka HTTP Server API]].
  * This results in creating a [[https://doc.akka.io/docs/akka/2.5/stream Stream]] of incoming session requests,
  * which produces a Stream for each session. These are materialized as child actors of this actor.
  *
  */
class RestActor(routes: Route) extends Configuration with Logging {
  logger.info("constructing...")

  var actorRefOption:  Option[ActorRef[RestActor.Message]] = None

  def getActorRef(): ActorRef[RestActor.Message] = {
    actorRefOption match {
      case None ⇒ throw new Exception
      case Some(actorRef) ⇒ actorRef
    }
  }

  val behavior: Behavior[Message] = Behaviors.setup { actorContext ⇒
    logger.info("initializing...")

    assert(actorContext != null)
    actorRefOption = Some(actorContext.self)

    // TODO remove this for production, used for testing
    //val cancelable = actorContext.schedule(200 seconds, actorContext.self, Done("timed out"))

    implicit val actorSystem = actorContext.system.toUntyped // compatibility with legacy Akka
    implicit val materializer = ActorMaterializer()
    implicit val executionContext = actorContext.system.executionContext

    val address = config.getRestAddress()
    val port = config.getRestPort()
    val curlAddress = config.getRestHostname()

    Http().bindAndHandle(routes, address, port).onComplete {
      case Success(result) ⇒
        logger.info(s"$result")
        logger.info(s"REST API bound to $address:$port\n\ntest with 'curl http://$curlAddress:$port/ping'\n\n")
      case Failure(cause) ⇒ cause.printStackTrace
    }

    Behaviors.receive[RestActor.Message] { (actorCell, message) ⇒
      logger.info(s"received $message")
      message match {
        case Done(cause) ⇒
          logger.info(s"Done: $cause")
          Behaviors.stopped
        case Fail(cause) ⇒
          throw cause
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
        Behaviors.same
    }
  }
}
