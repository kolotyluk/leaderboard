package net.kolotyluk.leaderboard.telemetry

import java.util.{ConcurrentModificationException, UUID}
import java.util.concurrent.atomic.AtomicLong

import net.kolotyluk.leaderboard.scorekeeping.MemberIdentifier
import net.kolotyluk.scala.extras.{Configuration, Internalized, Logging}

object Metrics extends Configuration with Logging {

  val msximumSpinCount = config.getLong("net.kolotyluk.leaderboard.maximumSpinCount")

  val maximumSpinCountExceeded = "maximumSpinCount = %d exceeded. This is probably caused because a lock was set on %s, but never removed, possibly because of thread failure."

  var largestSpinCount = new AtomicLong(0)
  var totalSpinCount = new AtomicLong(0)

  var largestSpinMember: MemberIdentifier = Internalized(new UUID(0,0))

  def checkSpinCount(memberIdentifier: MemberIdentifier, spinCount: Long) = {

    var currentCount = largestSpinCount.get
    while (spinCount > currentCount && !largestSpinCount.compareAndSet(currentCount, spinCount)) currentCount = largestSpinCount.get

    var total = totalSpinCount.get
    while (!totalSpinCount.compareAndSet(total, total + spinCount)) total = totalSpinCount.get

    if (spinCount > currentCount) largestSpinMember = memberIdentifier

      //largestSpinCount = spinCount
      //logger.info(s"spinCount = $spinCount, largestSpinCount = $largestSpinCount")


    if (spinCount > msximumSpinCount) {
      throw new ConcurrentModificationException(maximumSpinCountExceeded.format(msximumSpinCount, memberIdentifier))
    }
  }

  def getLargestSpinCount = largestSpinCount.get
  def getTotalSpinCount = totalSpinCount.get
  def getLargestSpinMember = largestSpinMember

  def resetTotalSpinCount = totalSpinCount.set(0)
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
