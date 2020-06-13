/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.service.api.registration

import akka.http.scaladsl.server.Directives._
import org.cafienne.identity.IdentityProvider
import org.cafienne.service.api.tenant.route.TenantRoute
import org.cafienne.tenant.akka.command.{AddTenantOwner, GetTenantOwners, RemoveTenantOwner}

class FormerTenantAdministrationRoute()(override implicit val userCache: IdentityProvider) extends TenantRoute {

  override def routes = {
    addTenantOwner ~
      removeTenantOwner ~
      getTenantOwners
  }

  def addTenantOwner = put {
    validUser { tenantOwner =>
      path(Segment / "owners" / Segment) { (tenant, userId) =>
        askTenant(tenantOwner, tenant, tenantUser => new AddTenantOwner(tenantUser, tenant, userId))
      }
    }
  }

  def removeTenantOwner = delete {
    validUser { tenantOwner =>
      path(Segment / "owners" / Segment) { (tenant, userId) =>
        askTenant(tenantOwner, tenant, tenantUser => new RemoveTenantOwner(tenantUser, tenant, userId))
      }
    }
  }

  def getTenantOwners = get {
    validUser { tenantOwner =>
      path(Segment / "owners") { tenant =>
        askTenant(tenantOwner, tenant, tenantUser => new GetTenantOwners(tenantUser, tenant))
      }
    }
  }
}
