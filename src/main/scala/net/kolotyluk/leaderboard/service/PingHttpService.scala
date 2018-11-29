package net.kolotyluk.leaderboard.service

import akka.http.scaladsl.server.Directives
import io.swagger.annotations._
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import javax.ws.rs.Path

@Path(value = "/ping")
trait PingHttpService
  extends Directives
    with ModelFormats {

//  @ApiOperation(httpMethod = "GET", response = classOf[Ping], value = "Returns a pet based on ID")
//  @ApiImplicitParams(Array(
//    new ApiImplicitParam(name = "petId", required = false, dataType = "integer", paramType = "path", value = "ID of pet that needs to be fetched")
//  ))
//  @ApiResponses(Array(
//    new ApiResponse(code = 400, message = "Invalid ID Supplied"),
//    new ApiResponse(code = 404, message = "Pet not found")))

  @Operation(summary = "Solicit a pong response",
    description = "Returns 'pong'",
    method = "GET",
    responses = Array(
      new ApiResponse(responseCode = "400", description = "Invalid ID supplied"),
      new ApiResponse(responseCode = "404", description = "Pet not found")
    )
  )
  def pingGetRoute = get { path("pet" / IntNumber) { petId =>
    complete("pong")
  } }
}

@ApiModel(description = "A pet object")
case class Ping()