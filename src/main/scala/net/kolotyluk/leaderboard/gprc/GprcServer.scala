package net.kolotyluk.leaderboard.gprc

import java.util.UUID

import io.grpc.ServerBuilder
import net.kolotyluk.leaderboard.Configuration
import net.kolotyluk.leaderboard.grpc.update.{UpdateRequest, UpdateResponse, UpdaterGrpc}
import net.kolotyluk.leaderboard.scorekeeping.{Increment, LeaderboardSync, Score}
import net.kolotyluk.scala.extras.{Internalized, Logging}

import scala.concurrent.{ExecutionContext, Future}

class GprcServer(implicit executionContext: ExecutionContext) extends Configuration with Logging { self =>

  private class UpdaterImpl extends UpdaterGrpc.Updater {
    override def update(request: UpdateRequest): Future[UpdateResponse] = {
      val leaderboardIdBuffer = request.leaderboardId.asReadOnlyByteBuffer()
      val leaderboardUuid = new UUID(leaderboardIdBuffer.getLong, leaderboardIdBuffer.getLong)

      val memberIdBuffer = request.memberId.asReadOnlyByteBuffer()
      val memberUuid = new UUID(memberIdBuffer.getLong, memberIdBuffer.getLong)


      val leaderboardIdentifier = Internalized[UUID](leaderboardUuid)
      val leaderboard = net.kolotyluk.leaderboard.akka_specific.endpoint.leaderboard.identifierToLeaderboard(leaderboardIdentifier)

      val memberIdentifier = Internalized[UUID](memberUuid)

      if (leaderboard.isInstanceOf[LeaderboardSync]) {
        Future {
          val score = leaderboard.update(Increment, memberIdentifier, BigInt(request.score)).asInstanceOf[Score]
          logger.debug(s"protobuf: leaderboardUuid = $leaderboardUuid, memberUuid = $memberUuid, score = ${score.value}")
          UpdateResponse(request.leaderboardId, request.memberId, score.value.toString)
        }
      } else {
        leaderboard.update(Increment, memberIdentifier, BigInt(request.score)).asInstanceOf[Future[Score]]
          .map {score =>
            logger.debug(s"protobuf: leaderboardUuid = $leaderboardUuid, memberUuid = $memberUuid, score = ${score.value}")
            UpdateResponse(request.leaderboardId, request.memberId, score.value.toString)
          }
      }
    }
  }

  def start() = {
    val server = ServerBuilder
      .forPort(config.getProtobufPort())
      .addService(UpdaterGrpc.bindService(new UpdaterImpl, executionContext))
      .build
      .start
    logger.info(s"Protocol Buffer Server started on port = ${config.getProtobufPort()}")
  }

}
