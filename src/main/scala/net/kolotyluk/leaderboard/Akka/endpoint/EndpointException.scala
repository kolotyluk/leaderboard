package net.kolotyluk.leaderboard.Akka.endpoint

import akka.http.scaladsl.model.HttpResponse

abstract class EndpointException(val response: HttpResponse) extends Exception {
  override def getMessage = response.toString()
}
