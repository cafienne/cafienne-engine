package org.cafienne.service.api

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.github.swagger.akka._
import com.github.swagger.akka.model.Info
import io.swagger.v3.oas.models.security.{OAuthFlow, OAuthFlows, Scopes, SecurityScheme}
import org.cafienne.akka.actor.CaseSystem
import org.cafienne.infrastructure.Configured
import org.cafienne.service.api.cases.CasesRoute
import org.cafienne.service.api.participants.{PlatformAdministrationRoute, RegistrationRoutes, TenantAdministrationRoute, TenantUsersAdministrationRoute}
import org.cafienne.service.api.repository.RepositoryRoute
import org.cafienne.service.api.tasks.TasksRoute

class SwaggerHttpServiceRoute(val system: ActorSystem) extends SwaggerHttpService with Configured {
  implicit val actorSystem: ActorSystem = system

  lazy val configuredHost = config.getString("cafienne.api.bindhost")
  lazy val configuredPort = config.getString("cafienne.api.bindport")

  override val apiClasses = Set[Class[_]](classOf[CaseServiceRoute],
    classOf[CasesRoute],
    classOf[TasksRoute],
    classOf[RepositoryRoute],
    classOf[TenantUsersAdministrationRoute],
    classOf[TenantAdministrationRoute],
    classOf[PlatformAdministrationRoute],
    classOf[RegistrationRoutes]
  )

  // override val host = s"$configuredHost:$configuredPort" //the url of your api, not swagger's json endpoint
  override val basePath = "/" //the basePath for the API you are exposing
  override val apiDocsPath = "api-docs" //where you want the swagger-json endpoint exposed
  override val info = Info(description =
    """HTTP JSON interface to the Cafienne APIs""".stripMargin, version = "1.0.0")

  val openIdSecurityScheme = new SecurityScheme()
    .name("openId")
    .`type`(SecurityScheme.Type.OPENIDCONNECT)
    .openIdConnectUrl(CaseSystem.OIDC.connectUrl)

  /* https://stackoverflow.com/questions/41918845/keycloak-integration-in-swagger
    "securityDefinitions": {
      "oauth2": {
          "type":"oauth2",
          "authorizationUrl":"http://172.17.0.2:8080/auth/realms/master/protocol/openid-connect/auth",
          "flow":"implicit",
          "scopes": {
              "openid":"openid",
              "profile":"profile"
          }
      }
    }
   */
  val oauth2ForOpenIdConnectHack = new SecurityScheme()
    .`type`(SecurityScheme.Type.OAUTH2)
    .name("openId")
    .flows(
      new OAuthFlows().`implicit`(
        new OAuthFlow()
          .scopes(
            new Scopes()
              .addString("openid", "openid")
              .addString("profile", "profile")
          )
          .tokenUrl(CaseSystem.OIDC.tokenUrl)
          //.extensions(Map("x-tokenName" -> "id_token").asInstanceOf[Map[String, AnyRef]].asJava) doesn't work with openapi 3
          .authorizationUrl(CaseSystem.OIDC.authorizationUrl)
      )
    )

  override def securitySchemes: Map[String, SecurityScheme] = Map("openId" -> oauth2ForOpenIdConnectHack)

  def swaggerUIRoute = get {
    routes ~
      pathPrefix("") {
        pathEndOrSingleSlash {
          getFromResource("swagger/index.html")
        }
      } ~ getFromResourceDirectory("swagger")
  }

}
