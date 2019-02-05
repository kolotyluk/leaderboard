package net.kolotyluk.akka.scala

import java.util.UUID

import akka.http.scaladsl.server.{PathMatcher, PathMatcher1}
import net.kolotyluk.leaderboard.Akka.endpoint.InvalidUrlIdException
import net.kolotyluk.scala.extras.base64UrlIdToUuid

package object extras {

  /** =Match UUID from Base 64 URL=
    *
    * ==Examples==
    * <pre>
    * // matches curl http://localhost:8080/foobar/fiqkXE39T_WcUcCPtGcoaQ/keAoZQECSwm0h7v6yw_3WQ
    * def leaderboardGet(): Route = {
    *   path("foobar" / base64uuid / base64uuid ~ PathEnd) { (uuidFoo, uuidBar)
    *     get {
    *       complete(getFooBar(uuidFoo, uuidBar))
    *     }
    *   }
    * </pre>
    *
    * @see [[https://doc.akka.io/docs/akka-http/current/routing-dsl/path-matchers.html Path Matchers]]
    * @see [[https://github.com/spray/spray/blob/master/spray-routing/src/main/scala/spray/routing/PathMatcher.scala GitHub]]
    */
  val base64uuid: PathMatcher1[UUID] =
    PathMatcher("""[a-zA-Z0-9_-]{22}""".r) flatMap { base64string =>
      try {
        Some(base64UrlIdToUuid(base64string))
      } catch {
        case cause: InvalidUrlIdException => None
      }
    }

}
