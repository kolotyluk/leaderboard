package net.kolotyluk.leaderboard.scorekeeping

trait LeaderboardSync extends Leaderboard {
  type Response[A] = A
}
