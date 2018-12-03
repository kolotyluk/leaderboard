package net.kolotyluk.leaderboard.swagger

import com.github.swagger.akka.SwaggerHttpService
import com.github.swagger.akka.model.Info
import net.kolotyluk.leaderboard.service.{LeaderboardService, PingService}

//object SwaggerDocService extends SwaggerHttpService {
//  override val apiClasses = Set(classOf[AddService], classOf[AddOptionService], classOf[HelloService], EchoEnumService.getClass)
//  override val host = "localhost:12345"
//  override val info = Info(version = "1.0")
//  override val externalDocs = Some(new ExternalDocumentation().description("Core Docs").url("http://acme.com/docs"))
//  //override val securitySchemeDefinitions = Map("basicAuth" -> new BasicAuthDefinition())
//  override val unwantedDefinitions = Seq("Function1", "Function1RequestContextFutureRouteResult")
//}

object SwaggerDocService extends SwaggerHttpService {
  override val apiClasses: Set[Class[_]] = Set(classOf[LeaderboardService], classOf[PingService])
  override val host = "localhost:8080" //the url of your api, not swagger's json endpoint
  override val apiDocsPath = "api-docs" //where you want the swagger-json endpoint exposed
  override val info = Info(version = "1.0") //provides license and other description details
  //override val externalDocs = Some(new ExternalDocumentation().description("Core Docs").url("http://acme.com/docs"))
}

