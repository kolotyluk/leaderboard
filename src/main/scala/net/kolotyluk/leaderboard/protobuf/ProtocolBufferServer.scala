package net.kolotyluk.leaderboard.protobuf

import io.grpc.ServerBuilder
import net.kolotyluk.leaderboard.Configuration
import net.kolotyluk.leaderboard.protobuf.update.{UpdateRequest, UpdateResponse, UpdaterGrpc}
import net.kolotyluk.scala.extras.Logging

import scala.concurrent.{ExecutionContext, Future}

class ProtocolBufferServer(executionContext: ExecutionContext) extends Configuration with Logging { self =>

  private class UpdaterImpl extends UpdaterGrpc.Updater {
    override def update(request: UpdateRequest) = {
      val response = UpdateResponse(request.leaderboardId, request.memberId, request.score)
      Future.successful(response)
    }
  }

  def start() = {
    val server = ServerBuilder
      .forPort(config.getProtobufPort())
      .addService(UpdaterGrpc.bindService(new UpdaterImpl, executionContext))
      .build
      .start
    logger.info("Protocol Buffer Server started...")
  }

}
