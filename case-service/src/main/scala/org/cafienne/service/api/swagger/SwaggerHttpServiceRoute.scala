package org.cafienne.service.api.swagger

import com.github.swagger.akka._
import com.github.swagger.akka.model.Info
import io.swagger.v3.oas.models.security.{OAuthFlow, OAuthFlows, Scopes, SecurityScheme}
import org.cafienne.infrastructure.{Cafienne, Configured}

class SwaggerHttpServiceRoute(override val apiClasses: Set[Class[_]]) extends SwaggerHttpService with Configured {

  lazy val configuredHost = config.getString("cafienne.api.bindhost")
  lazy val configuredPort = config.getString("cafienne.api.bindport")

  // override val host = s"$configuredHost:$configuredPort" //the url of your api, not swagger's json endpoint
  override val basePath = "/" //the basePath for the API you are exposing
  override val apiDocsPath = "api-docs" //where you want the swagger-json endpoint exposed
  override val info = Info(description =
    """HTTP JSON interface to the Cafienne APIs""".stripMargin, version = "1.0.0")

  val openIdSecurityScheme = new SecurityScheme()
    .name("openId")
    .`type`(SecurityScheme.Type.OPENIDCONNECT)
    .openIdConnectUrl(Cafienne.config.OIDC.connectUrl)

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
          .tokenUrl(Cafienne.config.OIDC.tokenUrl)
          //.extensions(Map("x-tokenName" -> "id_token").asInstanceOf[Map[String, AnyRef]].asJava) doesn't work with openapi 3
          .authorizationUrl(Cafienne.config.OIDC.authorizationUrl)
      )
    )

  override def securitySchemes: Map[String, SecurityScheme] = Map("openId" -> oauth2ForOpenIdConnectHack)

  def route = get {
    routes ~
      pathPrefix("") {
        pathEndOrSingleSlash {
          getFromResource("swagger/index.html")
        }
      } ~ getFromResourceDirectory("swagger")
  }

}
