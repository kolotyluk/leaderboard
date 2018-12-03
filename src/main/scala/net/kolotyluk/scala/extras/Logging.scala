package net.kolotyluk.scala.extras

import grizzled.slf4j.Logger

/** =Logging Behavior=
  * We use the [[http://software.clapper.org/grizzled-slf4j/ Grizzled Logger]]
  * so that we can use lazy
  * [[https://docs.scala-lang.org/overviews/core/string-interpolation.html Scala String Interpolation]]
  *
  */
trait Logging {
  /** This is lazy so that startup messaging can work without logging failures disrupting things. */
  lazy val logger = Logger(this.getClass)
}
