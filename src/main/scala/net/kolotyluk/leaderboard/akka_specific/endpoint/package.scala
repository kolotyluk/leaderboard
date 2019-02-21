package net.kolotyluk.leaderboard.akka_specific

import java.nio.ByteBuffer
import java.util.{Base64, UUID}

import akka.http.scaladsl.server.Route
import net.kolotyluk.leaderboard.akka_specific.swagger.SwaggerDocService
import net.kolotyluk.leaderboard.InternalIdentifier
import net.kolotyluk.scala.extras.Internalized

/** =Akka HTTP Endpoints=
  *
  * TODO do we want to have a /member endpoint?
  *
  * ==Examples==
  *
  * ===Ping===
  *
  * {{{
  * GET   /ping
  *       pong
  * }}}
  *
  * ===Create Leaderboard===
  *
  * {{{
  * POST   /leaderboard
  *        {"id":"DQz92Qw_ShCB-TMXf-wiAQ"}
  *
  * POST   /leaderboard?name=foo
  *        {"id":"DQz92Qw_ShCB-TMXf-wiAQ","name":"foo"}
  *
  * POST   /leaderboard {"kind":"LeaderboardActor","name":"foo"}
  *        {"id":"DQz92Qw_ShCB-TMXf-wiAQ","name":"foo"}
  * }}}
  *
  * ===Create Leaderboard Member===
  *
  * Note: creating members is optional, as they will be created implicitly, but it's the caller's responsibility
  * to use properly formatted member ids.
  *
  * {{{
  * POST   /leaderboard/member
  *        {"id":"cuWnCrtETqWtSC8rEYIiUA"}
  *
  * POST   /leaderboard/member?name=joe
  *        {"id":"cuWnCrtETqWtSC8rEYIiUA","name":"joe"}
  *
  * POST   /leaderboard/DQz92Qw_ShCB-TMXf-wiAQ
  *        {"id":"cuWnCrtETqWtSC8rEYIiUA"}
  *
  * POST   /leaderboard/DQz92Qw_ShCB-TMXf-wiAQ?name=joe
  *        {"id":"cuWnCrtETqWtSC8rEYIiUA","name":"joe"}
  * }}}
  *
  * ===Get Leaderboard Status===
  *
  * {{{
  * GET    /leaderboard
  *        {["id":"DQz92Qw_ShCB-TMXf-wiAQ"]}
  *
  * GET    /leaderboard/DQz92Qw_ShCB-TMXf-wiAQ
  *        {"id":"DQz92Qw_ShCB-TMXf-wiAQ","name":"foo"}
  *
  * GET    /leaderboard?name=foo
  *        {"id":"DQz92Qw_ShCB-TMXf-wiAQ","name":"foo"}
  * }}}
  *
  * {{{
  * POST   /leaderboard/DQz92Qw_ShCB-TMXf-wiAQ
  *        {"id":"DQz92Qw_ShCB-TMXf-wiAQ"}
  * POST   /leaderboard/DQz92Qw_ShCB-TMXf-wiAQ?name=foo
  *        {"id":"DQz92Qw_ShCB-TMXf-wiAQ","name":"foo"}
  * }}}
  *
  * ===Set Score===
  *
  * {{{
  * PUT   /leaderboard/DQz92Qw_ShCB-TMXf-wiAQ/cuWnCrtETqWtSC8rEYIiUA?score=1234
  * }}}
  *
  * ===Increment Score===
  *
  * {{{
  * PATCH /leaderboard/DQz92Qw_ShCB-TMXf-wiAQ/cuWnCrtETqWtSC8rEYIiUA?score=1234
  * }}}
  *
  * ===Get Score===
  *
  * {{{
  * GET   /leaderboard/DQz92Qw_ShCB-TMXf-wiAQ/cuWnCrtETqWtSC8rEYIiUA
  *       {"score":"2468"}
  * GET   /leaderboard/DQz92Qw_ShCB-TMXf-wiAQ/cuWnCrtETqWtSC8rEYIiUA {"score","standing"}
  *       {"score":"2468","standing":"0"}
  * }}}
  *
  * @see [[https://blog.mwaysolutions.com/2014/06/05/10-best-practices-for-better-restful-api 10 Best Practices for Better RESTful API]]
  */
package object endpoint {

  val failEndpoint = new FailEndpoint
  val pingEndpoint = new PingEndpoint

  val routes: Route =
    failEndpoint.route ~
      pingEndpoint.route ~
      leaderboard.leaderboardEndpoint.routes ~
      SwaggerDocService.routes

  def uuidToUrlId(uuid: UUID): String =
    Base64
      .getUrlEncoder
      .withoutPadding
      .encodeToString(ByteBuffer.allocate(16)
        .putLong(uuid.getMostSignificantBits())
        .putLong(uuid.getLeastSignificantBits())
        .array())

  def urlIdToUuid(id: String): UUID = {
    try {
      val byteBuffer = ByteBuffer.wrap(Base64.getUrlDecoder.decode(id))
      val high = byteBuffer.getLong
      val low = byteBuffer.getLong
      new UUID(high,low)
    } catch {
      case cause: IllegalArgumentException =>
        val exception = new InvalidUrlIdException(id)
        exception.initCause(cause)
        throw exception
    }
  }

  def internalIdentifierToUrlId(internalIdentifier: InternalIdentifier[UUID]): String = {
    if (internalIdentifier.value.isInstanceOf[UUID])
      uuidToUrlId(internalIdentifier.value.asInstanceOf[UUID])
    else
      throw new Exception()
  }

  def urlIdToInternalIdentifier(urlId: String): InternalIdentifier[UUID] = {
    val uuid = urlIdToUuid(urlId)
    Internalized(uuid)
  }

}

package endpoint {

  /** =Diagnostic Information=
    *
    * @param cause or symptom
    * @param diagnosis to remedy the issue
    * @param explanation URL pointing to a full explanation of the issue
    * @param systemLogMessage useful information when searching through system log files
    * @see [[https://blog.mwaysolutions.com/2014/06/05/10-best-practices-for-better-restful-api 10 Best Practices for Better RESTful API]]
    */
  final case class ErrorPayload(cause: String, diagnosis: String, explanation: String, systemLogMessage: String)

  final case class ErrorPayloads(errors: Seq[ErrorPayload])

}
