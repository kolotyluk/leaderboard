package unit

import net.kolotyluk.leaderboard.data.ScoreKeeper
import org.scalatest.FlatSpec

class ScoreKeeperSpec
  extends FlatSpec {

  behavior of "Leaderboard"

  it should "have count = 0" in {
    val leaderboard = new ScoreKeeper()
    assert(leaderboard.getCount() == 0)
  }

}
