package net.kolotyluk.leaderboard

import net.kolotyluk.scala.extras.{Configuration, Logging}

import _root_.scala.util.Random
import scala.collection.mutable.ArrayBuffer

package object scorekeeping extends Configuration with Logging {

  val msximumSpinCount = config.getLong("net.kolotyluk.leaderboard.maximumSpinCount")
  val maximumSpinCountExceeded = "{0}: maximumSpinCount = {1} exceeded. This is probably caused because a lock was set on {2}, but never removed, possibly because of thread failure."


  // TODO think about this to make sure collisions are unlikely, and how to detect them
  def randomLong: Long = Random.nextLong

  case class Placing(member: String, score: BigInt, place: Long)

  /** =getRange Result=
    *
    * [[Leaderboard.getRange()]] is a paged API, where the caller can iterate through the leaderboard
    * standings, one page at a time.
    *
    *
    * @param placings of members between start and stop of [[Leaderboard.getRange()]] call, where the size of
    *                 placings may be less than stop - start.
    * @param totalCount of members on the leaderboard
    */
  case class Range(placings: ArrayBuffer[Placing], totalCount: Long)


  /** =Leaderboard Score=
    * <p>
    * Leaderboard Score with Minimal Chances of Ties
    * <p>
    * The basis of tie-free scoring is random numbers in this implementation. While
    * [[https://redis.io/topics/data-types Redis Sorted Sets]]
    * that us the lexical ordering of the members to break ties, this is not really fair in a competition.
    * Basically, the ties will always be broken in favor of the lexical ordering of the member ID. Some Redis
    * leaderboard implementations us the update time to break ties, where either the first or last member
    * update wins in the tie. While this is valid, if the leaderboard service is split across multiple
    * runtimes, especially on different systems, the clock skew could unfairly bias the tie tie breaking.
    * Similarly, GUIDs or UUIDs are not fairly distributed.
    *
    * @param value
    * @param random
    */
  case class Score(value: BigInt, random: Long = randomLong) extends Comparable[Score] {

    override def compareTo(that: Score): Int = {
      if (this.value < that.value) -1
      else if (this.value > that.value) 1
      else if (this.random < that.random) -1
      else if (this.random > that.random) 1
      else 0
    }

    override def equals(obj: _root_.scala.Any): Boolean = {
      val that = obj.asInstanceOf[Score]
      this.value == that.value && this.random == that.random
    }
  }

  /** =Standing Place of Count=
    *
    * @param place
    * @param count
    */
  case class Standing(place: Int, count: Int)

  sealed trait UpdateMode
  case object Increment extends UpdateMode
  case object Replace extends UpdateMode

}


