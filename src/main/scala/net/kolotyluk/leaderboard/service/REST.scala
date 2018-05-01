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
import akka.http.scaladsl.server.Route
import net.kolotyluk.leaderboard.scorekeeping.Leaderboard
import akka.http.scaladsl.model.StatusCodes.BadRequest
import akka.http.scaladsl.model.StatusCodes.MethodNotAllowed
import akka.http.scaladsl.model.StatusCodes.NotFound

import scala.util.{Failure, Success}
import io.swagger.annotations._

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

      val routes: Route =
        PingService.route ~
        path("leaderboards") {
          get {
            complete(HttpEntity(ContentTypes.`text/plain(UTF-8)`, "pong"))
          }
        } ~
          LeaderboardService.routes ~
          SwaggerDocService.routes

      val bindingFuture = Http().bindAndHandle(routes, "localhost", 8888)

      //val bindingFuture = Http().bindAndHandle(routes, "localhost", 8888)

      println(s"Server online at http://localhost:8888 docs at http://localhost:8888/api-docs/swagger.json")

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
