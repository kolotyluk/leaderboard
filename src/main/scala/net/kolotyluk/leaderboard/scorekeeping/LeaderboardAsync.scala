package net.kolotyluk.leaderboard.scorekeeping

import scala.concurrent.Future
import scala.language.higherKinds

trait LeaderboardAsync extends Leaderboard {
  type AbstractResult[A] = Future[A]
}
