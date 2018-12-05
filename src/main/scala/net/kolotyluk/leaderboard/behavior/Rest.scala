package net.kolotyluk.leaderboard.behavior

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.adapter.TypedActorSystemOps
import akka.actor.typed.{Behavior, Terminated}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.RouteConcatenation
import akka.stream.ActorMaterializer
import net.kolotyluk.leaderboard.Configuration
import net.kolotyluk.leaderboard.service.{LeaderboardService, PingService}
import net.kolotyluk.leaderboard.swagger.SwaggerDocService
import net.kolotyluk.scala.extras.Logging

import scala.util.{Failure, Success}

/** =HTTP REST API=
  * <p>
  * Binds to HTTP port using [[https://doc.akka.io/docs/akka-http/current/server-side/index.html Akka HTTP Server API]].
  *
  */
class Rest extends RouteConcatenation with Configuration with Logging {
  logger.info("actor initializing...")

  sealed trait Message
  case class Done(cause: String) extends Message
  case class Spawn[M](behavior: Behavior[M], name:String) extends Message

  val behavior: Behavior[Message] = Behaviors.setup { actorContext ⇒
    logger.info("setting up...")

    // TODO remove this for production, used for testing
    //val cancelable = actorContext.schedule(200 seconds, actorContext.self, Done("timed out"))

    // val http = actorContext.spawn(Http.behavior, "Http")
    // actorContext.watch(http)

    implicit val actorSystem = actorContext.system.toUntyped // compatibility with legacy Akka
    implicit val materializer = ActorMaterializer()
    implicit val executionContext = actorContext.system.executionContext

    val routes =
        (new PingService).route ~
        (new LeaderboardService).routes ~
        SwaggerDocService.routes

    val address = config.getRestAddress()
    val port = config.getRestPort()

    Http().bindAndHandle(routes, address, port).onComplete {
      case Success(result) ⇒
        logger.info(s"$result")
        val curlAddress = address match {
          case "0.0.0.0" | "0:0:0:0:0:0:0:0" | "127.0.0.1" | "::1" ⇒ "localhost"
          case _ ⇒ address
        }
        logger.info(s"REST API bound to $address:$port\n\ntest with 'curl http://$curlAddress:$port/ping'\n\n")
      case Failure(cause) ⇒ cause.printStackTrace
    }

    Behaviors.receive[Message] { (actorCell, message) ⇒
      logger.info(s"received $message")
      message match {
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
        Behaviors.same
    }
  }
}
