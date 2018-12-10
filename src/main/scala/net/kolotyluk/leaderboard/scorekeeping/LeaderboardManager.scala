package net.kolotyluk.leaderboard.scorekeeping

import java.util.UUID

import scala.util.Try

trait LeaderboardManager {

  def add(): Try[ConcurrentLeaderboard]

  def add(name: String): Try[ConcurrentLeaderboard]

  def add(name: Option[String], uuid: UUID = UUID.randomUUID()): Try[ConcurrentLeaderboard]

  def get(uuid: UUID): Option[ConcurrentLeaderboard]

  def get(name: String): Option[ConcurrentLeaderboard]

  def getInfo(name: String): Option[Info]

  def getInfo(uuid: UUID): Option[Info]
}
