package net.kolotyluk.scala.extras

import java.util.Map.Entry

import scala.collection.JavaConverters._
import scala.collection.mutable

/** =Scala Environment Extras=
  * <p>
  * Some extra utilities for accessing the local system [[https://en.wikipedia.org/wiki/Environment_variable environment variables]].
  */
trait Environment {

  /** =Java Environment Variables=
    * Java friendly collection of environment variables.
    */
  lazy val env = System.getenv().entrySet()

  // TODO make this immutable?

  /** =Scala Environment Variables=
    * Scala friendly collection of environment variables.
    */
  lazy val environment:  mutable.Set[Entry[String, String]] =  env.asScala

  /** =Extra Methods=
    * Don't really need to use implicit here, but it makes it more consistent with [[net.kolotyluk.scala.extras.Configuration]]
    * @param environment
    */
  implicit class EnvironmentExtra(val environment:  mutable.Set[Entry[String, String]]) {

    /** =Environment Report=
      * <p>
      * Generate a report of known environment variables.
      * <p>
      * This is generally useful at application startup, and recommended before the logging system is invoked,
      * in case there are environment configuration issues leading to logging problems. For example
      * {{{
      * println(environment.getEnvironmentReport)
      * }}}
      */
    def getEnvironmentReport() = environment
      .map( entry => s"${entry.getKey} = ${entry.getValue}" )
      .toSeq
      .sorted
      .mkString("<environment>\n\t", "\n\t", "\n</environment>")
  }
}