package net.kolotyluk.leaderboard.Akka

import java.util
import java.util.UUID

import akka.actor.Scheduler
import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorContext, ActorRef, Behavior, SupervisorStrategy, Terminated}
import akka.util.Timeout
import net.kolotyluk.leaderboard.Akka.LeaderboardManagerActor.{Create, Spawn, Update}
import net.kolotyluk.leaderboard.Configuration
import net.kolotyluk.leaderboard.scorekeeping.{ConsecutiveLeaderboard, Leaderboard, LeaderboardIdentifier, MemberIdentifier, Score, UpdateMode}
import net.kolotyluk.scala.extras.Logging

import scala.collection.mutable
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps

object LeaderboardManagerActor {
  sealed trait Request
  case class Create(leaderboardIdentifier: LeaderboardIdentifier, replyTo: ActorRef[Leaderboard]) extends Request
  case class Update(leaderboard: UUID, member: String, updateMode: UpdateMode, score: Score) extends Request
  case class Spawn[M](behavior: Behavior[M], name:String) extends Request
}

/** =Outermost Behavior of ActorSystem=
  * Top level actor in Akka system, which spawns the next level of actors.
  * <p>
  * ==Supervision==
  * The main job of our Guardian Actor is to
  * [[https://doc.akka.io/docs/akka/2.5/typed/fault-tolerance.html#supervision supervise]]
  * lower level actors. In legacy Akka, if actors failed, such as from throwing an Exception, they would be
  * automatically restarted. In Akka Typed, the default action is not to restart child actors. Consequently,
  * we need to explicitly state our Supervision Policies.
  *
  * ===Initialization===
  *
  * The policy here is that if anything fails during Actor Initialization, we should consider that FATAL, and
  * shut down the system.
  *
  * ===Configuration===
  *
  * While most configuration happens during initialization, one conclusion would be that Configuration Errors
  * are also FATAL. However, configuration can happen at other times as well, such as when configuration can
  * change dynamically.
  *
  * @see [[https://doc.akka.io/docs/akka/2.5.18/typed/actors.html#introduction Akka Typed Introduction]]
  * @see [[https://doc.akka.io/docs/akka/2.5/typed/fault-tolerance.html#supervision Akka Typed Supervision]]
  */
class LeaderboardManagerActor() extends Configuration with Logging {
  logger.info("constructing...")

  var actorContext:  ActorContext[LeaderboardManagerActor.Request] = null
  var selfActorReference:  ActorRef[LeaderboardManagerActor.Request] = null

  implicit val timeout: Timeout = 3 seconds
  implicit var scheduler: Scheduler = null
  implicit var executionContext: ExecutionContext = null

  val leaderboardIdentifierToLeaderboard = new mutable.HashMap[LeaderboardIdentifier,LeaderboardActor]

  var restActorRef : ActorRef[RestActor.Message] = null

  val behavior: Behavior[LeaderboardManagerActor.Request] = Behaviors.setup { actorContext ⇒

    logger.info("initializing...")

    scheduler = actorContext.system.scheduler
    selfActorReference =  actorContext.self
    executionContext = actorContext.executionContext

    // TODO remove this for production, used for testing
    //val cancelable = actorContext.schedule(200 seconds, actorContext.self, Done("timed out"))

    Behaviors.receive[LeaderboardManagerActor.Request] { (actorCell, message) ⇒
      logger.debug(s"received $message")
      message match {
        case Create(leaderboardIdentifier: LeaderboardIdentifier, replyTo: ActorRef[Leaderboard]) ⇒
          try {
            //val leaderboardIdentifier = Internalized(UUID.randomUUID())
            val memberToScore = new util.HashMap[MemberIdentifier,Option[Score]]
            val scoreToMember = new util.TreeMap[Score,MemberIdentifier]
            val leaderboard = new ConsecutiveLeaderboard(leaderboardIdentifier, memberToScore, scoreToMember)
            val leaderboardActor = new LeaderboardActor(leaderboardIdentifier, leaderboard)
            val leaderboardActorRef = actorCell.spawn(leaderboardActor.behavior, s"leaderboard-${leaderboardIdentifier.value}")
            assert(leaderboardActorRef != null)
            Behaviors.supervise(leaderboardActor.behavior)
              .onFailure[ConfigurationError](SupervisorStrategy.stop)
              .orElse(Behavior.same)
            actorCell.watch(leaderboardActorRef)

            leaderboardIdentifierToLeaderboard.put(leaderboardIdentifier,leaderboardActor)

            replyTo ! leaderboardActor

            Behaviors.same
          } catch {
            case cause: AssertionError ⇒
              logger.error("Could not spawn Rest Behavior", cause)
              // TODO something better
              Behaviors.stopped
          }
        case Spawn(behavior, name) ⇒
          logger.info(s"spawning $name")
          val actorRef = actorCell.spawn(behavior, name)
          actorCell.watch(actorRef)
          //actorRef ! Start()
          Behaviors.same
        case Update(leaderboard: UUID, member: String, updateMode: UpdateMode, score: Score) =>
          Behaviors.same
      }
    } receiveSignal {
      case (actorContext, event) ⇒
        logger.warn(s"received signal with event = $event with actorContext = $actorContext")
        event match {
          case terminated@Terminated(actorRef) ⇒
            Behaviors.same
//            val failure = terminated.failure
//            logger.warn(s"actorRef = $actorRef, failure = $failure")
//            failure match {
//              case None ⇒
//                logger.error(s"FATAL stopping service because of unknown failure")
//                Behaviors.stopped
//              case Some(cause) ⇒
//                if (cause.isInstanceOf[ActorInitializationException]) {
//                  if (cause.getCause.isInstanceOf[ConfigurationError]) {
//                    // Constructing a ConfigurationError logs it's own diagnostics
//                    // Terminate things so that configuration problems can be resolved first
//                    logger.error(s"FATAL - stopping service because of ConfigurationError during Actor Initialization")
//                    Behaviors.stopped
//                  } else {
//                    // Any problem during Actor Initialization is probably transient and serious enough that it is
//                    // unwise to continue with the system. TODO: reconsider this
//                    logger.error(s"FATAL - stopping service because of ActorInitializationException", cause)
//                    Behaviors.stopped
//                  }
//                } else {
//                  logger.warn(s"unknown cause = $cause, continuing...")
//                  Behaviors.same
//                }
//              case _ ⇒
//                logger.warn(s"unknown failure = $failure, continuing...")
//                Behaviors.same
//            }
          case _ ⇒
            logger.warn(s"unknown event = $event, continuing...")
            Behaviors.same
        }
      case signal@_ ⇒
        logger.warn(s"unknown signal = $signal, continuing...")
        Behaviors.same
    }
  }

  def create(leaderboardIdentifier: LeaderboardIdentifier): Future[Leaderboard] = {
    selfActorReference ? (actorRef ⇒ Create(leaderboardIdentifier, actorRef))
  }
}
