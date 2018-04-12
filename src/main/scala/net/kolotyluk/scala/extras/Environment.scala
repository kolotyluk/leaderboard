package net.kolotyluk.scala.extras

import scala.collection.JavaConverters._


trait Environment {

  /** =Print Environment Report=
    * <p>
    * Send a report of environment variables to System.out
    * <p>
    * This is generally useful at application startup, and recommended before the logging system is invoked,
    * in case there are environment configuration issues leading to logging problems.
    */
  def printEnvironment = {
    val environmentReport = Environment.entrySet
      .map( entry => s"${entry.getKey} = ${entry.getValue}" )
      .toSeq
      .sorted
      .mkString("<environment>\n\t", "\n\t", "\n</environment>")
    println(environmentReport)
  }

}

object Environment {
  lazy val entrySet = System.getenv()
    .entrySet()
    .asScala
}