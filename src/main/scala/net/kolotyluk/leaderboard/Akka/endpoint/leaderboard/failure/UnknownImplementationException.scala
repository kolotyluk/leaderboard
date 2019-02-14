package net.kolotyluk.leaderboard.Akka.endpoint.leaderboard.failure

import akka.http.scaladsl.model.StatusCodes.BadRequest
import net.kolotyluk.leaderboard.Akka.endpoint.leaderboard.{Implementation, LeaderboardPostRequest}
import net.kolotyluk.leaderboard.Akka.endpoint.{EndpointException2, ErrorPayload}

/** =Unknown Leaderboard Implementation=
  *
  * When requesting to create a new leaderboard, the implementation specified could not be found.
  *
  *   1. Make sure you have spelled the implementation name correctly
  *   1. The response contains the names of all the supported implementations
  *
  * ==Examples==
  * {{{
  * unix shell: curl -H "Content-Type: application/json" -d '{"implementation":"Uknown"}' -X POST http://localhost:8080/leaderboard
  * Invoke-WebRequest http://localhost:8080/leaderboard -Method Post -ContentType "application/json" -Body '{"implementation":"Uknown"}'
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
  * @param requestJson
  * @param implementation name
  */

class UnknownImplementationException(requestJson: LeaderboardPostRequest, implementation: String)
  extends EndpointException2(BadRequest,
    ErrorPayload(
      cause = s"In $requestJson, $implementation is unknown!",
      diagnosis = s"Specify one of: ${Implementation.values.mkString(", ")}",
      explanation = "http://kolotyluk.github.io/projects/leaderboard/scaladocs/net/kolotyluk/leaderboard/Akka/endpoint/leaderboard/failure/UnknownLeaderboardIdentifierException.html",
      systemLogMessage = s"request to create leaderboard with unknown implementation=$implementation"))

