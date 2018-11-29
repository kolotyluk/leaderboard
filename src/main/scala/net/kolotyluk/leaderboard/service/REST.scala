package net.kolotyluk.leaderboard.service

//import akka.actor.{ActorContext, ActorRefFactory, ActorSystem}
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.adapter._
import akka.actor.typed.{Behavior, Terminated}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import net.kolotyluk.scala.extras.Logging

object REST extends Logging {
  logger.info("Initializing...")

  sealed trait Message
  case class Done(cause: String) extends Message
  // case class Spawn[M](behavior: Behavior[M], name:String) extends Message

  val behavior: Behavior[Message] = Behaviors.setup { actorContext ⇒
    logger.info(s"starting $actorContext")

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
      path("swagger") { getFromResource("swagger/index.html") } ~
      getFromResourceDirectory("swagger")
      //SwaggerDocService.routes

    val bindingFuture = Http().bindAndHandle(routes, "localhost", 8888)

    logger.info(s"Server online at http://localhost:8888 docs at http://localhost:8888/api-docs/swagger.json")

    Behaviors.receive[Message] { (actorCell, message) ⇒
      logger.info(s"received $message")
      message match {
        case message@Done(cause: String) =>
          logger.info(s"message = $message")
          logger.info(s"cause = $cause")
          Behaviors.stopped
      }
      Behaviors.same
    } receiveSignal {
      case (actorContext, Terminated(actorRef)) ⇒
        logger.info(s"actorContext = $actorContext")
        logger.info(s"actorRef = $actorRef")
        Behaviors.same
    }

  }

//  val behavior: Behavior[Message] = Behaviors.setup { actorContext ⇒
//      logger.info("starting...")
//
//      implicit val actorSystem = actorContext.system.toUntyped
//      implicit val actorMaterializer = ActorMaterializer()
//      implicit val executionContextExecutor = actorContext.executionContext
//
//      val routes: Route =
//        PingService.route ~
//        path("leaderboards") {
//          get {
//            complete(HttpEntity(ContentTypes.`text/plain(UTF-8)`, "pong"))
//          }
//        } ~
//          LeaderboardService.routes ~
//          SwaggerDocService.routes
//
//      val bindingFuture = Http().bindAndHandle(routes, "localhost", 8888)
//
//      //val bindingFuture = Http().bindAndHandle(routes, "localhost", 8888)
//
//      println(s"Server online at http://localhost:8888 docs at http://localhost:8888/api-docs/swagger.json")
//
//      Behaviors.receive[Message] { (actorCell, message) ⇒
//        logger.info(s"received $message")
//        message match {
//          case message@Done(cause: String) =>
//            Behaviors.stopped
//        }
//      } receiveSignal {
//        // There is no other information available with this signal.
//        // While akka knows the reason for termination, we don't.
//        case (actorCell, Terminated(actorRef)) ⇒
//          logger.warn(s"Terminated ${actorRef.path.name}")
//          Behaviors.same
//      }
//
//
//  }


}
