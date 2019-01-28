package net.kolotyluk.leaderboard.scorekeeping

import scala.util.Try

trait LeaderboardManager {

  def add(): Try[ConcurrentLeaderboard]

  def add(name: String): Try[ConcurrentLeaderboard]

  def add(name: Option[String], leaderboardIdentifier: LeaderboardIdentifier): Try[ConcurrentLeaderboard]

  def get(leaderboardIdentifier: LeaderboardIdentifier): Option[ConcurrentLeaderboard]

  def get(name: String): Option[ConcurrentLeaderboard]

  def getInfo(name: String): Option[LeaderboardInfo]

  def getInfo(leaderboardIdentifier: LeaderboardIdentifier): Option[LeaderboardInfo]
}
