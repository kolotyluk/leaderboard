package net.kolotyluk.leaderboard.akka_specific.endpoint.leaderboard.failure

import akka.http.scaladsl.model.StatusCodes.BadRequest
import net.kolotyluk.leaderboard.akka_specific.endpoint.{EndpointException2, ErrorPayload}

/** =Invalid Leaderboard Identifier=
  *
  * When referring to a leaderboard, the leaderboard ID specified could not be found.
  *
  *   1. Leaderboards must first explicitly be created before they are referenced
  *   1. If the leaderboard is deleted, so is its Identifier
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
  *                       "explanation": "http://kolotyluk.github.io/projects/leaderboard/scaladocs/net/kolotyluk/leaderboard/Akka/endpoint/leaderboard/failure/UnknownLeaderboardIdentifierException.html",
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
class InvalidUrlIdentifierException(urlId: String) // TODO - write unit test for this
  extends EndpointException2(BadRequest,
    ErrorPayload(
      cause = s"The specified identifier is invalid: urlId=$urlId.",
      diagnosis = "Check your code for how you generated this identifier. It must be a URL Safe, Base 64 encoded UUID",
      explanation = "http://kolotyluk.github.io/projects/leaderboard/scaladocs/net/kolotyluk/leaderboard/Akka/endpoint/leaderboard/failure/InvalidUrlIdentifierException.html",
      systemLogMessage = s"invalid URL identifier: urlId=$urlId, ")
  ) {

}
