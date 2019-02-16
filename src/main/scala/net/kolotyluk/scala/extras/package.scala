package net.kolotyluk.scala

import java.nio.ByteBuffer
import java.util.{Base64, UUID}

import scala.concurrent.{ExecutionContext, Future}
import scala.language.higherKinds

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
  *
  * ==Base 64 URL Identifiers==
  *
  * Sometimes it's nice to encode a 128-bit UUID as a 22-character Base 64 URL String, such as "keAoZQECSwm0h7v6yw_3WQ".
  * Normally a UUID is expressed as a 36-character string such as "91e02865-0102-4b09-b487-bbfacb0ff759", so this is a
  * simple way of saving 14 characters in URL encoding. For example
  * {{{
  * curl http://localhost/foo/keAoZQECSwm0h7v6yw_3WQ
  * }}}
  * Would internally refer to a resource for "foo/91e02865-0102-4b09-b487-bbfacb0ff759"
  */
package object extras {

  /** =Return a Future Result=
    *
    * Given a input value which can return either a value or a Future(value), this function always returns a Future
    * output value.
    *
    * ==Examples==
    * {{{
    * getFutureResult[Int,LeaderboardStatusResponse](
    *   leaderboard.getCount,
    *   count => LeaderboardStatusResponse(leaderboardUrlId, count))
    * }}}
    *
    * @param input value
    * @param output function
    * @param executor ExecutionContext for running Futures
    * @tparam I Input Type
    * @tparam O Output Type
    * @return Future with Output Result
    */
  def getFutureResult[I,O](input: Any, output: I => O)(implicit executor: ExecutionContext): Future[O] = {
    input match {
      case future: Future[I] @unchecked => // This needs to come first because of type erasure
        future.map(value => output(value))
      case value: I @unchecked =>
        try {
          Future.successful(output(value))
        } catch {
          case cause: Throwable =>
            Future.failed(cause)
        }
      // case result: Any =>
      //   throw new UnexpectedApiResultError(result, expecting = typeOf[A])
    }
  }

  /** =Generate Base 64 URL String from UUID=
    *
    * @param uuid Unique Universal Identifier
    * @return Base 64 URL String
    */
  def uuidToBase64UrlId(uuid: UUID): String =
    Base64
      .getUrlEncoder
      .withoutPadding
      .encodeToString(ByteBuffer.allocate(16)
        .putLong(uuid.getMostSignificantBits())
        .putLong(uuid.getLeastSignificantBits())
        .array())

  /** =Parse UUID from Base 64 URL String=
    *
    * @param id Base 64 URL String
    * @return Unique Universal Identifier
    * @throws InvalidBase64UrlToUuidException when parsing fails
    */
  def base64UrlIdToUuid(base64UrlId: String): UUID = {
    try {
      val byteBuffer = ByteBuffer.wrap(Base64.getUrlDecoder.decode(base64UrlId))
      val high = byteBuffer.getLong
      val low = byteBuffer.getLong
      new UUID(high,low)
    } catch {
      case cause: IllegalArgumentException =>
        if (base64UrlId.length != 22) {
          // TODO do we want to handle padding too?
          val message = s"base64UrlId = $base64UrlId does not contain exactly 22 characters"
          throw new InvalidBase64UrlToUuidException(message, cause)
        } else {
          val message = s"base64UrlId = $base64UrlId contains invalid base 64 URL characters. See also java.util.Base64"
          throw new InvalidBase64UrlToUuidException(message, cause)
        }
    }
  }
}

/** =Extra Classes=
  *
  * We put stuff here because the Scala compiler generates a warning if it's in the package object.
  */
package extras {

  class InvalidBase64UrlToUuidException(message: String, cause: Throwable) extends Exception() {

  }
}
