package net.kolotyluk.leaderboard.Akka.endpoint

import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.model.StatusCodes.BadRequest

class InvalidUrlIdException(id: String)
  extends EndpointException(HttpResponse(BadRequest, entity = s"id=$id is not properly formatted to parse a UUID from a Base 64 URL string")) {
  override def getMessage = response.toString()
}
