package net.kolotyluk.leaderboard.Akka

import java.util.UUID

import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorContext, ActorRef, Behavior, Terminated}
import akka.actor.{ActorInitializationException, Scheduler}
import akka.util.Timeout
import net.kolotyluk.leaderboard.Akka.LeaderboardActor._
import net.kolotyluk.leaderboard.scorekeeping.{LeaderboardAsync, LeaderboardSync, Score, Standing, UpdateMode}
import net.kolotyluk.leaderboard.{Configuration, scorekeeping}
import net.kolotyluk.scala.extras.{Identity, Logging}

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps

object LeaderboardActor {
  sealed trait Request
  case class Delete(member: String, replyTo: ActorRef[Boolean]) extends Request
  case class GetCount(replyTo: ActorRef[Int]) extends Request
  case class GetInfo(replyTo: ActorRef[scorekeeping.Info]) extends Request
  case class GetName(replyTo: ActorRef[Option[String]]) extends Request
  case class GetRange(start: Long, stop: Long, replyTo: ActorRef[scorekeeping.Range]) extends Request
  case class GetScore(member: String, replyTo: ActorRef[Option[BigInt]]) extends Request
  case class GetStanding(member: String, replyTo: ActorRef[Option[Standing]]) extends Request
  // case class Update(member: String, updateMode: UpdateMode, score: Score) extends Request
  case class Update(member: String, updateMode: UpdateMode, score: Score, replyTo: ActorRef[Score]) extends Request
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
class LeaderboardActor(leaderboard: LeaderboardSync) extends LeaderboardAsync with Configuration with Logging {
//  uuid = initialUUID
//  name = initialName

  logger.info("constructing...")

  var actorContext:  ActorContext[LeaderboardActor.Request] = null
  var selfActorReference:  ActorRef[LeaderboardActor.Request] = null

  implicit val timeout: Timeout = 3 seconds
  implicit var scheduler: Scheduler = null
  implicit var executionContext: ExecutionContext = null


//  val memberToScore = new util.HashMap[String,Option[Score]]
//  val scoreToMember = new ConcurrentSkipListMap[Score,String]
//  val leaderboard = new ConsecutiveLeaderboard(memberToScore, scoreToMember)

  var restActorRef : ActorRef[RestActor.Message] = null

  val behavior: Behavior[LeaderboardActor.Request] = Behaviors.setup { actorContext ⇒

    logger.info("initializing...")

    scheduler = actorContext.system.scheduler
    selfActorReference =  actorContext.self
    executionContext = actorContext.executionContext

    // TODO remove this for production, used for testing
    //val cancelable = actorContext.schedule(200 seconds, actorContext.self, Done("timed out"))

    Behaviors.receive[LeaderboardActor.Request] { (actorCell, message) ⇒
      logger.debug(s"received $message")
      message match {
        case Delete(member: String, replyTo: ActorRef[Boolean]) ⇒
          replyTo ! leaderboard.delete(member)
          Behaviors.same
        case GetCount(replyTo: ActorRef[Int]) ⇒
          replyTo ! leaderboard.getCount
          Behaviors.same
        case GetInfo(replyTo: ActorRef[scorekeeping.Info]) ⇒
          replyTo ! leaderboard.getInfo
          Behaviors.same
        case GetName(replyTo: ActorRef[Option[String]]) ⇒
          replyTo ! leaderboard.getName
          Behaviors.same
        case GetRange(start: Long, stop: Long, replyTo: ActorRef[scorekeeping.Range]) ⇒
          replyTo ! leaderboard.getRange(start, stop)
          Behaviors.same
        case GetScore(member: String, replyTo: ActorRef[Option[BigInt]]) ⇒
          replyTo ! leaderboard.getScore(member)
          Behaviors.same
        case GetStanding(member: String, replyTo: ActorRef[Option[Standing]]) ⇒
          replyTo ! leaderboard.getStanding(member)
          Behaviors.same
        case Update(member: String, updateMode: UpdateMode, score: Score, replyTo: ActorRef[Score]) ⇒
          replyTo ! leaderboard.update(updateMode, member, score)
          Behaviors.same
        case Spawn(behavior, name) ⇒
          logger.info(s"spawning $name")
          val actorRef = actorCell.spawn(behavior, name)
          actorCell.watch(actorRef)
          //actorRef ! Start()
          Behaviors.same
      }
    } receiveSignal {
      case (actorContext, event) ⇒
        logger.warn(s"received signal with event = $event with actorContext = $actorContext")
        event match {
          case terminated@Terminated(actorRef) ⇒
            val failure = terminated.failure
            logger.warn(s"actorRef = $actorRef, failure = $failure")
            failure match {
              case None ⇒
                logger.error(s"FATAL stopping service because of unknown failure")
                Behaviors.stopped
              case Some(cause) ⇒
                if (cause.isInstanceOf[ActorInitializationException]) {
                  if (cause.getCause.isInstanceOf[ConfigurationError]) {
                    // Constructing a ConfigurationError logs it's own diagnostics
                    // Terminate things so that configuration problems can be resolved first
                    logger.error(s"FATAL - stopping service because of ConfigurationError during Actor Initialization")
                    Behaviors.stopped
                  } else {
                    // Any problem during Actor Initialization is probably transient and serious enough that it is
                    // unwise to continue with the system. TODO: reconsider this
                    logger.error(s"FATAL - stopping service because of ActorInitializationException", cause)
                    Behaviors.stopped
                  }
                } else {
                  logger.warn(s"unknown cause = $cause, continuing...")
                  Behaviors.same
                }
              case _ ⇒
                logger.warn(s"unknown failure = $failure, continuing...")
                Behaviors.same
            }
          case _ ⇒
            logger.warn(s"unknown event = $event, continuing...")
            Behaviors.same
        }
      case signal@_ ⇒
        logger.warn(s"unknown signal = $signal, continuing...")
        Behaviors.same
    }
  }

  override def delete(member: String) =
    selfActorReference ? (actorRef ⇒ Delete(member, actorRef))

  override def getCount=
    selfActorReference ? (actorRef ⇒ GetCount(actorRef))

  override def getInfo =
    selfActorReference ? (actorRef ⇒ GetInfo(actorRef))

  override def getName =
    selfActorReference ? (actorRef ⇒ GetName(actorRef))

  override def getRange(start: Long, stop: Long): Future[scorekeeping.Range] =
    selfActorReference ? (actorRef ⇒ GetRange(start: Long, stop: Long, actorRef))

  override def getScore(member: String) =
    selfActorReference ? (actorRef ⇒ GetScore(member, actorRef))

  override def getStanding(member: String) =
    selfActorReference ? (actorRef ⇒ GetStanding(member, actorRef))

  override def getUrlIdentifier(identifier: String) = Future{Identity.getUrlIdentifier(identifier)}
  override def getUrlIdentifier(uuid: UUID) = Future{Identity.getUrlIdentifier(uuid)}
  override def getUuid = Future{uuid}

  override def update(mode: UpdateMode, member: String, value: BigInt) = update(mode, member, Score(value))

  override def update(mode: UpdateMode, member: String, newScore: Score) =
    selfActorReference ? (actorRef ⇒ Update(member, mode, newScore, actorRef))
}
