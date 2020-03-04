/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.service.api.tenant.route

import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import akka.http.scaladsl.server.Directives._
import org.cafienne.identity.IdentityProvider
import org.cafienne.service.api.tenant.UserQueries

class  DeprecatedUserInfoRoute(userQueries: UserQueries)(override implicit val userCache: IdentityProvider) extends TenantRoute {

  override def routes = {
    getUserInformation
  }

  def getUserInformation = get {
    pathPrefix("user-information") {
      pathEndOrSingleSlash {
        validUser { user =>
          val value = HttpEntity(ContentTypes.`application/json`, user.toJSON)
          complete(StatusCodes.OK, value)
        }
      }
    }
  }
}
