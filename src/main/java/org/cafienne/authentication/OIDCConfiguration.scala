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

package org.cafienne.authentication

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.oauth2.sdk.id.Issuer
import com.nimbusds.openid.connect.sdk.SubjectType
import com.nimbusds.openid.connect.sdk.op.OIDCProviderMetadata
import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging
import org.cafienne.infrastructure.config.util.ConfigReader
import org.cafienne.json.JSONReader

import java.net.URI
import java.util
import java.util.Collections

class OIDCConfiguration(override val config: Config) extends ConfigReader {
  val issuer: String = readString("issuer")
  val metadata: Option[OIDCProviderMetadata] = {
    if (config.hasPath("key-url")) {
      // static configuration
      logger.info(s"Reading static info for IDP $issuer")
      Some(readStaticConfiguration())
    } else {
      // dynamic configuration; perhaps issuer differs from connect-url, when connect-url is set, use that.
      val connectUrl: String = readString("connect-url")
      if (connectUrl.nonEmpty) {
        logger.info(s"Reading dynamic info for IDP $issuer from connect-url $connectUrl")
        Some(readDynamicConfiguration(connectUrl))
      } else if (issuer.nonEmpty) {
        logger.info(s"Reading dynamic info for IDP $issuer")
        Some(readDynamicConfiguration(issuer))
      } else {
        logger.warn(s"Encountered empty IDP configuration; this configuration will be skipped")
        None
      }
    }
  }

  def readStaticConfiguration(): OIDCProviderMetadata = {
    val keysUrl: String = readString("key-url")
    val subjectTypes = new util.ArrayList[SubjectType]()
    subjectTypes.add(SubjectType.PUBLIC)

    val metadata = new OIDCProviderMetadata(new Issuer(this.issuer), subjectTypes, new URI(keysUrl))

    // The expected JWS algorithm of the access tokens (agreed out-of-band ... but why not configurable with default...)
    val expectedJWSAlg: JWSAlgorithm = JWSAlgorithm.RS256 // TODO: could this become configurable as well?
    metadata.setIDTokenJWSAlgs(Collections.singletonList(expectedJWSAlg))

    // These properties are used in SwaggerUI
    val authorizationUrl: String = readString("authorization-url")
    metadata.setAuthorizationEndpointURI(new URI(authorizationUrl))
    val tokenUrl: String = readString("token-url")
    metadata.setTokenEndpointURI(new URI(tokenUrl))

    metadata
  }

  def readDynamicConfiguration(endpoint: String): OIDCProviderMetadata = {
    //    println(s"\nRetrieving metadata info for IDP Issuer ${config.issuer} from well known url ${config.connectUrl} ")
    val metadata: OIDCProviderMetadata = OIDCProviderMetadata.resolve(new Issuer(endpoint))
    logger.info(s"Retrieved dynamic info from IDP $endpoint: " + JSONReader.parse(metadata.toJSONObject.toJSONString()))
    metadata
  }
}

object OIDCConfiguration extends LazyLogging {
  def readConfigurations(configs: Seq[Config]): Seq[OIDCProviderMetadata] = {
    val issuers = configs.map(new OIDCConfiguration(_)).filter(_.metadata.nonEmpty).map(_.metadata.get)
    // Check that we have actual values in the list
    logger.warn(s"Cafienne HTTP Server is configured with ${issuers.size} identity providers: ${issuers.map(_.getIssuer.toString).mkString("\n- ", "\n- ", "")}")
    issuers
  }
}
