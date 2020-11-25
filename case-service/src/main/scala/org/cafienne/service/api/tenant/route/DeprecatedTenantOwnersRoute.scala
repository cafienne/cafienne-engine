/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.service.api.tenant.route

import akka.http.scaladsl.server.Directives._
import org.cafienne.identity.IdentityProvider
import org.cafienne.service.api.projection.query.UserQueries
import org.cafienne.service.api.tenant.model.TenantAPI
import org.cafienne.tenant.akka.command._

class DeprecatedTenantOwnersRoute(userQueries: UserQueries)(override implicit val userCache: IdentityProvider) extends TenantRoute {

  // POST Method has been replaced with PUT method. Keeping this for compatibility
  override def routes = {
      addTenantUser
  }

  def addTenantUser = post {
    validUser { platformUser =>
      path(Segment / "users") { tenant =>
        import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
        import spray.json.DefaultJsonProtocol._
        implicit val format = jsonFormat6(TenantAPI.UserFormat)
        entity(as[TenantAPI.UserFormat]) { newUser =>
          extractUri { uri =>
            logger.warn(s"Using deprecated POST on $uri to update Tenant User. Please use PUT instead of POST")
            askTenant(platformUser, tenant, tenantOwner => new UpsertTenantUser(tenantOwner, asTenantUser(newUser, tenant)))
          }
        }
      }
    }
  }
}
