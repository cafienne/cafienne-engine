/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.service.api.registration

import akka.http.scaladsl.server.Directives._
import org.cafienne.akka.actor.identity.TenantUser
import org.cafienne.identity.IdentityProvider
import org.cafienne.service.api.tenant.model.TenantAPI
import org.cafienne.service.api.tenant.route.TenantRoute
import org.cafienne.tenant.akka.command.platform.{CreateTenant, DisableTenant, EnableTenant}

import scala.collection.JavaConverters._

class FormerPlatformAdministrationRoute()(override implicit val userCache: IdentityProvider) extends TenantRoute {


  override def routes = {
    createTenant ~
    disableTenant ~
    enableTenant
  }

  def createTenant = post {
    pathEndOrSingleSlash {
      validUser { platformOwner =>
        import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
        import spray.json.DefaultJsonProtocol._

        implicit val userFormat = jsonFormat4(TenantAPI.User)
        implicit val tenantFormat = jsonFormat2(TenantAPI.Tenant)
        entity(as[TenantAPI.Tenant]) { newTenant =>
          val newTenantName = newTenant.name
          val owners = newTenant.owners.map(owner => TenantUser(owner.userId, owner.roles.toSeq, newTenantName, owner.name.getOrElse(""), owner.email.getOrElse(""), true))
          askTenant(new CreateTenant(platformOwner, newTenantName, newTenantName, owners.asJava))
        }
      }
    }
  }
  def disableTenant = put {
    validUser { platformOwner =>
      path(Segment / "disable") { tenant =>
        askTenant(new DisableTenant(platformOwner, tenant.name))
      }
    }
  }

  def enableTenant = put {
    validUser { platformOwner =>
      path(Segment / "disable") { tenant =>
        askTenant(new EnableTenant(platformOwner, tenant.name))
      }
    }
  }
}
