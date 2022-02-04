/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.service.api.anonymous

import io.swagger.v3.oas.annotations.security.SecurityRequirement
import org.cafienne.identity.IdentityProvider
import org.cafienne.system.CaseSystem

import javax.ws.rs._

@SecurityRequirement(name = "openId", scopes = Array("openid"))
@Path("/request")
class AnonymousRequestRoutes(implicit val userCache: IdentityProvider, override implicit val caseSystem: CaseSystem) extends AnonymousRoute {

  override val prefix: String = "request"

  addSubRoute(new CaseRequestRoute())
}
