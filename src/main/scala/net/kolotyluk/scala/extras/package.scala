package net.kolotyluk.scala

/** =Extra Utilities=
  * Extra utilities for Scala developers.
  * <p>
  * Some things it might have been nice to see in the standard Scala libraries, but are offered here instead.
  * For example:
  * {{{
  * object Main
  *   extends App
  *     with Configuration
  *     with Environment
  *     with Logging {
  *
  *   // Safest way to indicate something is happening, don't rely on logging yet
  *   println(s"Starting ${getClass.getName}...")
  *
  *   println("Reporting environment and configuration for troubleshooting purposes")
  *   println(environment.getEnvironmentReport())
  *   println(config.getConfigurationReport())
  *
  *   // If logging is broken, hopefully there is enough output now for a diagnosis
  *   logger.info("Logging started")
  * }
  * }}}
  */
package object extras {

}
