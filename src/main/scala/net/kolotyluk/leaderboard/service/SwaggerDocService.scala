package net.kolotyluk.leaderboard.service

import com.github.swagger.akka.SwaggerHttpService
import com.github.swagger.akka.model.Info
import io.swagger.models.ExternalDocs
import io.swagger.models.auth.BasicAuthDefinition

object SwaggerDocService extends SwaggerHttpService {
  override val apiClasses = Set(LeaderboardService.getClass, PingService.getClass)
  override val host = "localhost:8888"
  override val info = Info(version = "1.0")
  override val externalDocs = Some(new ExternalDocs("Github Repo", "https://github.com/kolotyluk/leaderboard"))
  override val securitySchemeDefinitions = Map("basicAuth" -> new BasicAuthDefinition())
  override val unwantedDefinitions = Seq("Function1", "Function1RequestContextFutureRouteResult")
}