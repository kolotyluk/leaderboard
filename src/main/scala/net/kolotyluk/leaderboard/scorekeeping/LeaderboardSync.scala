package net.kolotyluk.leaderboard.scorekeeping

import scala.language.higherKinds

trait LeaderboardSync extends Leaderboard {
  type AbstractResult[A] = A
}
