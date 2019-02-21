package net.kolotyluk.leaderboard.akka_specific.endpoint

import akka.http.scaladsl.model.{HttpResponse, StatusCode}

abstract class EndpointException(val response: HttpResponse) extends Exception {
  override def getMessage = response.toString()
}

abstract class EndpointError(val statusCode: StatusCode, val errorPayload: ErrorPayload) extends Error


abstract class EndpointException2(val statusCode: StatusCode, val errorPayload: ErrorPayload) extends Exception
