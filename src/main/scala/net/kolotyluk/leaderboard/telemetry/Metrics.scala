package net.kolotyluk.leaderboard.telemetry

import java.util.ConcurrentModificationException
import java.util.concurrent.atomic.AtomicLong

import net.kolotyluk.scala.extras.{Configuration, Logging}

object Metrics extends Configuration with Logging {

  val msximumSpinCount = config.getLong("net.kolotyluk.leaderboard.maximumSpinCount")

  val maximumSpinCountExceeded = "maximumSpinCount = %d exceeded. This is probably caused because a lock was set on %s, but never removed, possibly because of thread failure."

  var largestSpinCount = new AtomicLong(0)
  var totalSpinCount = new AtomicLong(0)

  var largestSpinMember: String = "no one"

  def checkSpinCount(member: String, spinCount: Long) = {

    var currentCount = largestSpinCount.get

    while (spinCount > currentCount && !largestSpinCount.compareAndSet(currentCount, spinCount)) {
      currentCount = largestSpinCount.get
    }

    if (spinCount > currentCount) largestSpinMember = member

      //largestSpinCount = spinCount
      //logger.info(s"spinCount = $spinCount, largestSpinCount = $largestSpinCount")


    if (spinCount > msximumSpinCount) {
      throw new ConcurrentModificationException(maximumSpinCountExceeded.format(msximumSpinCount, member))
    }
  }

  def getLargestSpinCount = largestSpinCount.get
  def getLargestSpinMember = largestSpinMember
  def resetLargestSpinCount = largestSpinCount.set(0)

  var largestSpinTime = new AtomicLong(0)
  var totalSpinTime = new AtomicLong(0)

  def checkSpinTime(spinTime: Long) = {

    var time = largestSpinTime.get
    while (spinTime > time && !largestSpinTime.compareAndSet(time, spinTime)) time = largestSpinTime.get

    var total = totalSpinTime.get
    while (!totalSpinTime.compareAndSet(total, total + spinTime)) total = totalSpinTime.get

      //logger.info(s"spinTime = $spinTime, largestSpinTime = $largestSpinTime")
  }

  def getLargestSpinTime = largestSpinTime.get
  def getTotalSpinTime = totalSpinTime.get

  def resetLargetsSpinTime = largestSpinTime.set(0)
  def resetTotalSpinTime = totalSpinTime.set(0)
}
