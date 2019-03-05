package net.kolotyluk.leaderboard.akka_specific

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.adapter.TypedActorSystemOps
import akka.actor.typed.{ActorRef, Behavior, Terminated}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import net.kolotyluk.leaderboard.Configuration
import net.kolotyluk.leaderboard.akka_specific.RestActor._
import net.kolotyluk.scala.extras.Logging

import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.{Failure, Success}

object RestActor {
  sealed trait Message
  case class Done(cause: String) extends Message
  case class Fail(cause: Throwable) extends Message
  case class Spawn[M](behavior: Behavior[M], name:String) extends Message
  case class Unbind(deadline: FiniteDuration, replyTo: ActorRef[Http.HttpTerminated]) extends Message
}

/** =HTTP REST API=
  * Binds to one or more interfaces listening for HTTP Requests
  *
  * ==Akka HTTP==
  *
  * Binds using [[https://doc.akka.io/docs/akka-http/current/server-side/index.html Akka HTTP Server API]].
  * This results in creating a [[https://doc.akka.io/docs/akka/2.5/stream Stream]] of incoming session requests,
  * which produces a Stream for each session. These are materialized as child actors of this actor.
  *
  * ==Graceful Termination==
  *
  * The main advantage of Graceful Termination, via unbind, is that if there are pending 'in-flight' requests,
  * they can be completed before the system shuts down.
  *
  * ==Coordinated Shutdown==
  *
  * This is fairly complex, and not handled in Akka HTTP, and not clear how to handle it in Akka Typed.
  * This is just a placeholder for now...
  *
  * @see [[https://doc.akka.io/docs/akka-http/current/server-side/graceful-termination.html Graceful Termination]]
  * @see [[https://doc.akka.io/docs/akka/2.5.19/actors.html#coordinated-shutdown Coordinated Shutdown]]
  */
class RestActor(routes: Route) extends Configuration with Logging {
  logger.info("constructing...")

  var actorRefOption:  Option[ActorRef[RestActor.Message]] = None
  var serverBinding: Option[Http.ServerBinding] = None

  // TODO do we need this?
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

    Http().bindAndHandle(routes, address, port) onComplete {
      case Success(binding) ⇒
        serverBinding = Some(binding)
        logger.info(s"$binding")
        logger.info(s"REST API bound to $address:$port\n\ntest with 'curl http://$curlAddress:$port/ping'\n\n")
      case Failure(cause) ⇒
        logger.error(cause)
    }

    Behaviors.receive[RestActor.Message] { (actorCell, message) ⇒
      logger.info(s"received $message")
      message match {
        case Done(cause) => // TODO do we need this?
          Behaviors.same
        case Fail(cause) ⇒
          throw cause
        case Spawn(behavior, name) ⇒
          logger.info(s"spawning $name")
          val actorRef = actorCell.spawn(behavior, name)
          actorCell.watch(actorRef)
          //actorRef ! Start()
          Behaviors.same
        case Unbind(deadline: FiniteDuration, replyTo: ActorRef[Http.HttpTerminated]) ⇒
          logger.info(s"Received request to unbind. Starting Graceful Termination of HTTP API.")
          logger.warn(s"Unbinding from $address:$port with deadline = $deadline")
          serverBinding.foreach{ binding =>
            binding.terminate(deadline).map{ unbound =>
              replyTo ! unbound
              serverBinding = None
            }
          }
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
