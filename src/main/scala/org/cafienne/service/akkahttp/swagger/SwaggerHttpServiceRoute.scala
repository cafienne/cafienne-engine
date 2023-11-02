/*
 * Copyright (C) 2014  Batav B.V.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.cafienne.service.akkahttp.swagger

import com.github.swagger.akka._
import com.github.swagger.akka.model.Info
import io.swagger.v3.oas.models.security.{OAuthFlow, OAuthFlows, Scopes, SecurityScheme}
import org.cafienne.infrastructure.Cafienne

class SwaggerHttpServiceRoute(override val apiClasses: Set[Class[_]]) extends SwaggerHttpService {

   //override val host = s"$configuredHost:$configuredPort" //the url of your api, not swagger's json endpoint
  override val host = s"${Cafienne.config.api.bindHost}:${Cafienne.config.api.bindPort}"
  override val basePath = "/" //the basePath for the API you are exposing
  override val apiDocsPath = "api-docs" //where you want the swagger-json endpoint exposed
  override val info = Info(description =
    """HTTP JSON interface to the Cafienne APIs""".stripMargin, version = "1.0.0")

  val oidc = Cafienne.config.OIDC.issuers.head

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
    .flows(
      new OAuthFlows().`implicit`(
        new OAuthFlow()
          .scopes(
            new Scopes()
              .addString("openid", "openid")
              .addString("profile", "profile")
          )
          .authorizationUrl(oidc.getAuthorizationEndpointURI.toString)
      )
    )

  override def securitySchemes: Map[String, SecurityScheme] = Map(SecurityScheme.Type.OAUTH2.toString -> oauth2ForOpenIdConnectHack)

  //override def security: List[SecurityRequirement] = List(new SecurityRequirement().addList("openId"))
  def route = get {
    routes ~
      pathPrefix("") {
        pathEndOrSingleSlash {
          getFromResource("swagger/index.html")
        }
      } ~ getFromResourceDirectory("swagger")
  }

}
