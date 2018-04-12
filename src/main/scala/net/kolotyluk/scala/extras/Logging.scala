package net.kolotyluk.scala.extras

import grizzled.slf4j.Logger

trait Logging {
  /** This is lazy so that startup messaging can work without logging failures disrupting things. */
  lazy val logger = Logger(this.getClass)
}
