package net.kolotyluk.scala.extras

import com.typesafe.config.ConfigFactory
import scala.collection.JavaConverters._


trait Configuration {

  val config = ConfigFactory.load

  /** =Print Configuration Report=
    * <p>
    * Send a report of configuration properties to System.out
    * <p>
    * This is generally useful at application startup, and recommended before the logging system is invoked,
    * in case there are configuration issues leading to logging problems.
    */
  def printConfiguration = {

    val configurationReport = config
      .entrySet
      .asScala.map(entry => s"${entry.getKey} = ${entry.getValue.render}")
      .toSeq
      .sorted
      .mkString("<configuration>\n\t", "\n\t", "\n</configuration>")
    println(configurationReport)
  }

}
