package net.kolotyluk.scala.extras

import java.awt.geom.IllegalPathStateException

import com.typesafe.config.Config

import scala.collection.JavaConverters._

/** =Enhanced Typesafe Config=
  * Extra features for [[https://github.com/lightbend/config Typesafe Config]]
  * <p>
  * It is recommended that you extend this trait in your app, in the same style, but for local configuration.
  * For example:
  * {{{
  * trait Configuration extends net.kolotyluk.scala.extras.Configuration {
  *
  *   config.setPathBase("net.flybynight.myapp")
  *
  *   implicit class LocalConfiguration(val config: com.typesafe.config.Config) extends Logging  {
  *     import com.typesafe.config.Config
  *
  *     // net.flybynight.myapp.akka.system.name
  *     def getAkkaSystemName(default: Some[String] = Some("myapp")): String  = config.getDefaultString("akka.system.name", default)
  *     def getRestAddress(default: Some[String] = Some("0.0.0.0")) : String = config.getDefaultString("rest.address", default)
  *     def getRestPort(default: Some[Int] = Some(8080)) : Int = config.getDefaultInt("rest.port", default, 0 to 65535)
  *   }
  * }
  * }}}
  * Good [[https://en.wikipedia.org/wiki/Separation_of_concerns Separation of Concerns]]
  * practice would be to make your configuration code as robust as possible. You main application should not handle
  * any configuration problems, rather all configuration troubleshooting should be in your Configuration trait. In the
  * main body of code you might use:
  * {{{
  * val akkaSystemName = config.getAkkaSystemName()
  * val restAddress = config.getRestAddress()
  * val restPort = config.getRestPort()
  * }}}
  */
  trait Configuration extends Logging {

  /** =Fatal Configuration Error=
    * Indicates and unrecoverable configuration problem was encountered.
    * <p>
    * A ConfigurationError indicates an unrecoverable condition was encountered during configuration, and that the
    * application or service should be terminated. Configuration problems are usually human error when initially
    * setting up an application or service, so it's generally best to fail big, and fail early, so that the problem
    * can be remedied.
    *
    * @param path from pathBase, of configuration setting
    * @param value value found
    * @param message diagnosis and prognosis
    * @param cause underlying Throwable
    */
  class ConfigurationError(path: String, value: Any, message: String, cause: Throwable) extends Error {
    def this(path: String, value: Any, message: String) = this(path, value, message, null)
    logger.error(s"Configuration error at ${PathBase.pathBase}.$path = $value; check your reference.conf or application.conf settings")
    logger.error(message, cause)
  }

  /** =Enhanced Typesafe Config=
    * The basic Typesafe Config with extra implicit members defined on it.
    */
  val config = com.typesafe.config.ConfigFactory.load

  /** =Implicit Local Configuration=
    *
    * @param config Typesafe config
    */
  implicit class ExtraConfiguration(val config: com.typesafe.config.Config) extends Logging {

    def getConfigurationReport(): String = {
      config
        .entrySet
        .asScala
        .map(entry => s"${entry.getKey} = ${entry.getValue.render}")
        .toSeq
        .sorted
        .mkString("<configuration>\n\t", "\n\t", "\n</configuration>")
    }

    def setPathBase(path: String) = {
      PathBase.setPathBase(path)
    }

    private def base(config: Config) = {
      try {
        if (PathBase.pathBase == null) {
          val message = "\n\n\nHave you forgotten to call config.setPathBase? Using pathBase = \"\" instead.\n\n\n"
          logger.error(message)
          throw new IllegalPathStateException(message)
        } else {
            config.getConfig(PathBase.pathBase)
        }
      }  catch {
        case cause: com.typesafe.config.ConfigException.Missing ⇒
          logger.error(s"You need to define '${PathBase.pathBase}' in reference.conf or application.conf", cause)
          throw cause
        case cause: com.typesafe.config.ConfigException.WrongType ⇒
          logger.error("Internal problem with Typesafe Configuration handling", cause)
          throw cause
      }
    }

    /** =Get Default Int=
      *
      * @param path
      * @param default
      * @param interval
      * @return
      * @throws java.lang.IllegalArgumentException
      */
    def getDefaultInt(path: String, default: Option[Int], range: Range ): Int = {
      default match {
        case None ⇒
          throw new IllegalArgumentException("Default value missing from configuration.")
        case Some(default) ⇒
          try {
            val result = base(config).getInt(path)
            if (result < range.start) throw new Exception(s"${PathBase.pathBase}.$path must not be less than ${range.start}! Using default = $default")
            if (result > range.last)  throw new Exception(s"${PathBase.pathBase}.$path must not be greater than ${range.last}! Using default = $default")
            result
          } catch {
            case cause: com.typesafe.config.ConfigException.Missing =>
              logger.warn("Using $pathBase.$path = $default.get. Check reference.conf or application.conf for proper configuration", cause)
              default
            case cause: com.typesafe.config.ConfigException.WrongType =>
              logger.warn("Using $pathBase.$path = $default.get. Check reference.conf or application.conf for proper configuration", cause)
              default
            case cause: Exception =>
              logger.warn(cause.getMessage)
              default
          }
      }
    }

    def getDefaultString(path: String, default: Option[String]): String = {
      try {
        base(config).getString(path)
      } catch {
        case cause: com.typesafe.config.ConfigException.Missing =>
          logger.warn("Using $pathBase.$path = $default.get. Check reference.conf or application.conf for proper configuration", cause)
          default.get
        case cause: com.typesafe.config.ConfigException.WrongType =>
          logger.warn("Using $pathBase.$path = $default.get. Check reference.conf or application.conf for proper configuration", cause)
          default.get
      }
    }

  }

  object PathBase extends Logging {
    var pathBase: String = null
    def setPathBase(path: String) = {
      pathBase = path
      // TODO log this later...
      // logger.info(s"pathBase set to ${pathBase}")
    }
  }

}
