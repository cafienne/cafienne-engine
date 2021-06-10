/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.service.api.identifiers.route

import io.swagger.v3.oas.annotations.security.SecurityRequirement
import org.cafienne.identity.IdentityProvider
import org.cafienne.infrastructure.akka.http.route.QueryRoute
import org.cafienne.service.api.cases.CaseReader
import org.cafienne.service.db.query.IdentifierQueries
import org.cafienne.system.CaseSystem

import javax.ws.rs._

@SecurityRequirement(name = "openId", scopes = Array("openid"))
@Path("/identifiers")
class IdentifierRoutes(val identifierQueries: IdentifierQueries)(override implicit val userCache: IdentityProvider, override implicit val caseSystem: CaseSystem) extends QueryRoute {
  override val lastModifiedRegistration = CaseReader.lastModifiedRegistration
  override val prefix = "identifiers"

  addSubRoute(new IdentifiersRoute(identifierQueries)(userCache, caseSystem))
}
