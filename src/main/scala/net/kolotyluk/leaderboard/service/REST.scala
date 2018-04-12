package net.kolotyluk.leaderboard.service

//import akka.actor.{ActorContext, ActorRefFactory, ActorSystem}
import akka.actor.typed.{Behavior, Terminated}
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import net.kolotyluk.scala.extras.Logging

import akka.actor.typed.scaladsl.adapter._

object REST extends Logging {
  logger.info("Initializing...")

  sealed trait Message
  case class Done(cause: String) extends Message
  //case class Spawn[M](behavior: Behavior[M], name:String) extends Message

  val behavior: Behavior[Message] = Behaviors.setup {
    actorContext ⇒
      logger.info("starting...")

      implicit val actorSystem = actorContext.system.toUntyped
      implicit val actorMaterializer = ActorMaterializer()
      implicit val executionContextExecutor = actorContext.executionContext

      val route =
        path("hello") {
          get {
            complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, "<h1>Say hello to akka-http</h1>"))
          }
        } ~
        path("ping") {
          get {
            complete(HttpEntity(ContentTypes.`text/plain(UTF-8)`, "pong"))
          }
        }

      val bindingFuture = Http().bindAndHandle(route, "localhost", 8888)

      //val bindingFuture = Http().bindAndHandle(route, "localhost", 8888)

      println(s"Server online at http://localhost:8888/\nPress RETURN to stop...")

      Behaviors.immutable[Message] { (actorCell, message) ⇒
        logger.info(s"received $message")
        message match {
          case message@Done(cause: String) =>
            Behaviors.stopped
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
