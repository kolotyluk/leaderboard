package net.kolotyluk.leaderboard

import java.net.{InetAddress, UnknownHostException}

import net.kolotyluk.scala.extras.Logging

/** =Local Configuration=
  * Central point of all leaderboard configuration
  * <p>
  * Extends [[net.kolotyluk.scala.extras.Configuration]]
  * that extends [[https://github.com/lightbend/config Typesafe Config]], so that pattern should be followed.
  */
trait Configuration extends net.kolotyluk.scala.extras.Configuration {

  config.setPathBase("net.kolotyluk.leaderboard")

  /** =Implicit Local Configuration=
    *
    * @param config Typesafe config
    */
  implicit class LocalConfiguration(val config: com.typesafe.config.Config) extends Logging  {

    /** =Akka System Name=
      *
      * @param default "leaderboard" akka system name if not found in config files
      * @return configured akka system name
      */
    def getAkkaSystemName(default: Some[String] = Some("leaderboard")): String  = config.getDefaultString("akka.system.name", default)

    /** =HTTP Rest Address=
      *
      * 127.0.0.1 is normally the IP address assigned to the "loopback" or local-only interface.
      * This is a "fake" network adapter that can only communicate within the same host. It's often
      * used when you want a network-capable application to only serve clients on the same host.
      * A process that is listening on 127.0.0.1 for connections will only receive local connections
      * on that socket.
      * <p>
      * 0.0.0.0 has a couple of different meanings, but in this context, when a server is told to
      * listen on 0.0.0.0 that means "listen on every available network interface". The loopback
      * adapter with IP address 127.0.0.1 from the perspective of the server process looks just like
      * any other network adapter on the machine, so a server told to listen on 0.0.0.0 will accept
      * connections on that interface too.
      *
      * @param default "0.0.0.0" akka system name if not found in config files
      * @return configured rest address
      */
    def getRestAddress(default: Some[String] = Some("0.0.0.0")) : String = {
      val path = "rest.address"
      val address = config.getDefaultString(path, default)
      try {
        val inetAddress = InetAddress.getByName(address)
        logger.info(s"rest.address = $address, inetAddress = $inetAddress")
        address
      } catch {
        case cause: UnknownHostException ⇒
          val message = s"host not found at $address"
          logger.error(message, cause)
          throw new ConfigurationError(path, address,  message, cause)
      }
    }

    /**=HTTP Rest Hostname=
      * The hostname for the URL
      *
      * @return
      */
    def getRestHostname() : String = {
      val address = getRestAddress()
      address match {
        case "0.0.0.0" | "0:0:0:0:0:0:0:0" | "127.0.0.1" | "::1" ⇒ "localhost"
        case _ ⇒
          val iNetAddress = InetAddress.getByName(address)
          iNetAddress.getHostName
      }
    }

    /** =HTTP Rest Port=
      *
      * @param default 8080 akka system name if not found in config files
      * @return configured rest port
      */
    def getRestPort(default: Some[Int] = Some(8080)) : Int = config.getDefaultInt("rest.port", default, 0 to 65535)

    /** =Protocol Buffer Port=
      *
      * @param default 8080 akka system name if not found in config files
      * @return configured rest port
      */
    def getProtobufPort(default: Some[Int] = Some(8081)) : Int = config.getDefaultInt("protobuf.port", default, 0 to 65535)
  }
}
