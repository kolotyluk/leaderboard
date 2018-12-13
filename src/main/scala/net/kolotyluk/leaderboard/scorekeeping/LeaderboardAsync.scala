package net.kolotyluk.leaderboard.scorekeeping

import scala.concurrent.Future

trait LeaderboardAsync extends Leaderboard {
  type Response[A] = Future[A]
}
