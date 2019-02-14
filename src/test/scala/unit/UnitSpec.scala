package unit

import net.kolotyluk.scala.extras.Logging

import org.scalatest._

/** =Base Unit Test=
  *
  */
abstract class UnitSpec
  extends FlatSpec
    with SequentialNestedSuiteExecution
    with GivenWhenThen
    with Matchers
    with Logging

