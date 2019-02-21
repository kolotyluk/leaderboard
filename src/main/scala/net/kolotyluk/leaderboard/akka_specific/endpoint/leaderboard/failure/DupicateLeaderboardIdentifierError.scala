package net.kolotyluk.leaderboard.akka_specific.endpoint.leaderboard.failure

import java.util.UUID

import akka.http.scaladsl.model.StatusCodes.InternalServerError
import net.kolotyluk.leaderboard.akka_specific.endpoint.{EndpointError, ErrorPayload}
import net.kolotyluk.leaderboard.scorekeeping.LeaderboardIdentifier
import net.kolotyluk.scala.extras.uuidToBase64UrlId

/** =Duplicate Leaderboard Identifier=
  *
  * When attempting to create a new leaderboard, this leaderboard identifier already exists.
  *
  *   1. Leaderboard identifiers are only created by the software
  *   1. It's a programming defect to try to create another with the same identifier
  *   1. It's not a API user error
  *   1. It's not a fatal error, just an indication of an internal defect.
  *
  * ==Examples==
  * {{{
  * unix shell: curl http://localhost:8080/leaderboard/fiqkXE39T_WcUcCPtGcoaQ
  * PowerShell: Invoke-WebRequest -Method Get http://localhost:8080/leaderboard/fiqkXE39T_WcUcCPtGcoaQ
  * }}}
  * results in:
  * {{{
  * StatusCode        : 404
  * StatusDescription : Not Found
  * Content           : {
  *                       "cause": "The specified leaderboard cannot be found: urlId=IMvWdANITIWZxm7efUKVAg, uuid=20cbd674-0348-4c85-99c6-6ede7d429502.",
  *                       "diagnosis": = "Did you forget to create it?",
  *                       "explanation": "http://kolotyluk.github.io/projects/leaderboard/scaladocs/net/kolotyluk/leaderboard/Akka/endpoint/leaderboard/UnknownLeaderboardIdentifierException.html",
  *                       "systemLogMessage": "unknown leaderboard: urlId=IMvWdANITIWZxm7efUKVAg, uuid=20cbd674-0348-4c85-99c6-6ede7d429502"
  *                     }
  * }}}
  * ===Solutions===
  * First create a leaderboard
  * {{{
  * unix shell: curl -d "" http://localhost:8080/leaderboard
  * PowerShell: Invoke-WebRequest -Method Post http://localhost:8080/leaderboard
  * }}}
  * You should see something like:
  * {{{
  * StatusCode        : 201
  * StatusDescription : Created
  * Content           : {
  *                       "id": "a3Kyn9UXQj6FiFJaPR9ZyA"
  *                     }
  * }}}
  * Check your solution:
  * {{{
  * unix shell: curl http://localhost:8080/leaderboard/a3Kyn9UXQj6FiFJaPR9ZyA
  * PowerShell: Invoke-WebRequest -Method Get http://localhost:8080/leaderboard/a3Kyn9UXQj6FiFJaPR9ZyA
  * }}}
  * You should see something like:
  * {{{
  * StatusCode        : 200
  * StatusDescription : OK
  * Content           : {
  *                       "id": "a3Kyn9UXQj6FiFJaPR9ZyA",
  *                       "size": 0
  *                     }
  * }}}
  *
  * @param uuid internal leaderboard UUID
  * @param urlId external Base 64 URL Id
  */
class DupicateLeaderboardIdentifierError(uuid: UUID, urlId: String)
  extends EndpointError(InternalServerError,
      ErrorPayload(
        cause = s"Attempt to create a leaderboard with an existing identifier: urlId=$urlId, uuid=$uuid.",
        diagnosis = "Internal system error due to software defect. Contact customer support.",
        explanation = "http://kolotyluk.github.io/projects/leaderboard/scaladocs/net/kolotyluk/leaderboard/Akka/endpoint/leaderboard/failure/DupicateLeaderboardIdentifierError.html",
        systemLogMessage = s"duplicate leaderboard identifier: urlId=$urlId, uuid=$uuid")
    )
{
  def this(uuid: UUID) {
    this(uuid, uuidToBase64UrlId(uuid))
  }

  def this(leaderboardIdentifier: LeaderboardIdentifier) {
    this(leaderboardIdentifier.value)
  }
}
