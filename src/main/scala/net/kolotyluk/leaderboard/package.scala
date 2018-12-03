package net.kolotyluk

import net.kolotyluk.scala.extras.Logging

package object leaderboard{

  /** =Implicit Local Configuration=
    *
    * @param config Typesafe config
    */
  implicit class LocalConfiguration(val config: com.typesafe.config.Config) extends Logging  {
    import com.typesafe.config.Config

    val pathBase = "net.kolotyluk.leaderboard"

    private def base(config: Config) = {
      try {
        config.getConfig(pathBase)
      }  catch {
        case cause: com.typesafe.config.ConfigException.Missing =>
          logger.error(s"You need to define '$pathBase' in reference.conf or application.conf", cause)
          throw cause
        case cause: com.typesafe.config.ConfigException.WrongType =>
          logger.error("Internal problem with Typesafe Configuration handling", cause)
          throw cause
      }
    }

    def getDefaultInt(path: String, default: Int, interval: (Int,Int) ): Int = {
      try {
        val result = base(config).getInt(path)
        if (result < interval._1) throw new Exception(s"$pathBase.$path must not be less than ${interval._1}! Using default = $default")
        if (result > interval._2) throw new Exception(s"$pathBase.$path must not be greater than ${interval._2}! Using default = $default")
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

    def getDefaultString(path: String, default: Some[String]): String = {
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

    /** =Akka System Name=
      *
      * @param default akka system name if not found in config files
      * @return configured akka system name
      */
    def getAkkaSystemName(default: Some[String] = Some("leaderboard")): String  = getDefaultString("akka.system.name", default)

    def getRestAddress(default: Some[String] = Some("0.0.0.0")) : String = getDefaultString("rest.address", default)

    def getRestPort(default: Some[String] = Some("0.0.0.0")) : Int = getDefaultInt("rest.port", 8080, (0, 65535))
  }

}
