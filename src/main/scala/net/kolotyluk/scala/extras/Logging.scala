package net.kolotyluk.scala.extras

import grizzled.slf4j.Logger

/** =Extra Logging Behavior=
  * It is recommended that you extend this trait in your app, in the same style, but for local configuration.
  */
trait Logging {

  /** =Grizzled Logger=
    * The [[http://software.clapper.org/grizzled-slf4j/ Grizzled Logger]]
    * so that we can use lazy
    * [[https://docs.scala-lang.org/overviews/core/string-interpolation.html Scala String Interpolation]]
    * For example:
    * {{{
    * val foo = "foo"
    * val bar = 2
    * logger.debug(s"@foo $bar")
    * }}}
    * Where `s"$foo $bar"` is only evaluated if logging level is "DEBUG" or higher
    * <p>
    * This is lazy so that startup messaging can work without logging failures disrupting things. For example
    * [[net.kolotyluk.scala.extras.]]
    */
  lazy val logger = Logger(this.getClass)
}
