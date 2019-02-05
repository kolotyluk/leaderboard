package net.kolotyluk.leaderboard.Akka.swagger

import com.github.swagger.akka.SwaggerHttpService
import com.github.swagger.akka.model.Info
import io.swagger.v3.oas.models.ExternalDocumentation
import net.kolotyluk.leaderboard.Akka.endpoint.{PingEndpoint}
import net.kolotyluk.leaderboard.Akka.endpoint.leaderboard.LeaderboardEndpoint

object SwaggerDocService extends SwaggerHttpService {
  override val apiClasses: Set[Class[_]] = Set(classOf[LeaderboardEndpoint], classOf[PingEndpoint])
  override val host = "localhost:8080" //the url of your api, not swagger's json endpoint
  override val apiDocsPath = "api-docs" //where you want the swagger-json endpoint exposed
  override val info = Info(version = "1.0") //provides license and other description details
  override val externalDocs = Some(new ExternalDocumentation().description("Core Docs").url("http://net.kolotyluk/docs"))
}

