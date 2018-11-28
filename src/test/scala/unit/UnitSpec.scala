package net.kolotyluk.leaderboard.scorekeeping

import net.kolotyluk.scala.extras.Logging

import org.scalatest._

abstract class UnitSpec
  extends FlatSpec
    with SequentialNestedSuiteExecution
    with GivenWhenThen
    with Matchers
    with Logging