package net.kolotyluk.leaderboard.service

import com.github.swagger.akka.SwaggerHttpService
import com.github.swagger.akka.model.Info

//object SwaggerDocService extends SwaggerHttpService {
//  override val apiClasses = Set(LeaderboardService.getClass, PingService.getClass)
//  override val host = "localhost:8888"
//  override val info = Info(version = "1.0")
//  // override val externalDocs = Some(new ExternalDocumentation("Github Repo", "https://github.com/kolotyluk/leaderboard"))
//  override val externalDocs = Some(new ExternalDocumentation())
//  override val securitySchemeDefinitions = Map("basicAuth" -> new BasicAuthDefinition())
//  override val unwantedDefinitions = Seq("Function1", "Function1RequestContextFutureRouteResult")
//}

object SwaggerDocService extends SwaggerHttpService {
  override val apiClasses: Set[Class[_]] = Set(LeaderboardService.getClass, PingService.getClass)
  override val host = "localhost:8080" //the url of your api, not swagger's json endpoint
  //override val basePath = "/"    //the basePath for the API you are exposing
  override val apiDocsPath = "api-docs" //where you want the swagger-json endpoint exposed
  override val info = Info() //provides license and other description details
}
