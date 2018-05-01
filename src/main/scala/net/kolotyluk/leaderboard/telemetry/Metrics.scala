package net.kolotyluk.leaderboard.telemetry

import java.util.ConcurrentModificationException
import java.util.concurrent.atomic.AtomicLong

import net.kolotyluk.scala.extras.{Configuration, Logging}

object Metrics extends Configuration with Logging {

  val msximumSpinCount = config.getLong("net.kolotyluk.leaderboard.maximumSpinCount")
  val maximumSpinCountExceeded = "maximumSpinCount = {1} exceeded. This is probably caused because a lock was set on {2}, but never removed, possibly because of thread failure."

  var largestSpinCount = new AtomicLong(0)

  def checkSpinCount(spinCount: Long) = {

    var currentCount = largestSpinCount.get

    while (spinCount > currentCount && !largestSpinCount.compareAndSet(currentCount, spinCount)) {
      currentCount = largestSpinCount.get
    }

      //largestSpinCount = spinCount
      //logger.info(s"spinCount = $spinCount, largestSpinCount = $largestSpinCount")


    if (spinCount > msximumSpinCount) {
      throw new ConcurrentModificationException(maximumSpinCountExceeded.format())
    }
  }

  def getLargestSpinCount = largestSpinCount.get

  var largestSpinTime = new AtomicLong(0)


  def checkSpinTime(spinTime: Long) = {

    var time = largestSpinTime.get

    while (spinTime > time && !largestSpinTime.compareAndSet(time, spinTime)) {
      time = largestSpinTime.get
    }

      //logger.info(s"spinTime = $spinTime, largestSpinTime = $largestSpinTime")
  }

  def getLargestSpinTime = largestSpinTime.get
}
