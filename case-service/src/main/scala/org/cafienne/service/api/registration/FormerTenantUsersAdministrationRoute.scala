/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.service.api.registration

import akka.http.scaladsl.marshalling.Marshaller
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import akka.http.scaladsl.server.Directives._
import org.cafienne.akka.actor.identity.TenantUser
import org.cafienne.cmmn.instance.casefile.ValueList
import org.cafienne.identity.IdentityProvider
import org.cafienne.service.api.tenant.UserQueries
import org.cafienne.service.api.tenant.model._
import org.cafienne.service.api.tenant.route.TenantRoute
import org.cafienne.tenant.akka.command._

import scala.collection.JavaConverters._
import scala.util.{Failure, Success}

class FormerTenantUsersAdministrationRoute(userQueries: UserQueries)(override implicit val userCache: IdentityProvider) extends TenantRoute {

  override def routes = {
    addTenantUser ~
      addTenantUserRoles ~
      removeTenantUserRole ~
      enableTenantUser ~
      disableTenantUser ~
      getTenantUsers ~
      getTenantUser ~
      getUserInformation
  }

  def addTenantUser = post {
    validUser { platformUser =>
      path(Segment / "users") { tenant =>
        import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
        import spray.json.DefaultJsonProtocol._
        implicit val format = jsonFormat4(TenantAPI.User)
        entity(as[TenantAPI.User]) { newUser =>
            val roles = newUser.roles.asJava
            val name = newUser.name.getOrElse("")
            val email = newUser.email.getOrElse("")
            askTenant(platformUser, tenant, tenantOwner => new AddTenantUser(tenantOwner, tenant, newUser.userId, roles, name, email))
        }
      }
    }
  }

  def disableTenantUser = put {
    validUser { tenantOwner =>
      path(Segment / "users" / Segment / "disable") { (tenant, userId) =>
        askTenant(tenantOwner, tenant, tenantUser => new DisableTenantUser(tenantUser, tenant, userId))
      }
    }
  }

  def enableTenantUser = put {
    validUser { tenantOwner =>
      path(Segment / "users" / Segment / "enable") { (tenant, userId) =>
        askTenant(tenantOwner, tenant, tenantUser => new EnableTenantUser(tenantUser, tenant, userId))
      }
    }
  }

  def addTenantUserRoles = put {
    validUser { tenantOwner =>
      path(Segment / "users" / Segment / "roles" / Segment) { (tenant, userId, role) =>
        askTenant(tenantOwner, tenant, tenantUser => new AddTenantUserRole(tenantUser, tenant, userId, role))
      }
    }
  }

  def removeTenantUserRole = delete {
    validUser { platformUser =>
      path(Segment / "users" / Segment / "roles" / Segment) { (tenant, userId, role) =>
        askTenant(platformUser, tenant, tenantUser => new RemoveTenantUserRole(tenantUser, tenant, userId, role))
      }
    }
  }

  def getTenantUsers = get {
    validUser { platformUser =>
      path(Segment / "users") { tenant =>
        onComplete(userQueries.getTenantUsers(platformUser, tenant)) {
          case Success(users) =>
            implicit val usersMarshaller = Marshaller.withFixedContentType(ContentTypes.`application/json`) { users: Seq[TenantUser] =>
              val vList = new ValueList()
              users.foreach(u => vList.add(u.toJson))
              HttpEntity(ContentTypes.`application/json`, vList.toString)
            }
            complete(StatusCodes.OK, users)
          case Failure(err) =>
            err match {
              case err: SecurityException => complete(StatusCodes.Unauthorized, err.getMessage)
              case _ => complete(StatusCodes.InternalServerError, err)
            }
        }
      }
    }
  }

  def getTenantUser = get {
    validUser { platformUser =>
      path(Segment / "users" / Segment) { (tenant, userId) =>
        platformUser.shouldBelongTo(tenant)

        onComplete(userQueries.getPlatformUser(userId)) {
          case Success(requestedUser) =>
            val tenantUserInformation = requestedUser.getTenantUser(tenant)
            implicit val tenantUserMarshaller = Marshaller.withFixedContentType(ContentTypes.`application/json`) { user: TenantUser =>
              HttpEntity(ContentTypes.`application/json`, user.toJson.toString)
            }

            complete(StatusCodes.OK, tenantUserInformation)
          case Failure(err) =>
            err match {
              case err: SecurityException => complete(StatusCodes.Unauthorized, err.getMessage)
              case _ => complete(StatusCodes.InternalServerError, err)
            }
        }
      }
    }
  }

  def getUserInformation = get {
    pathPrefix("user-information") {
      pathEndOrSingleSlash {
        validUser { platformUser =>
          val value = HttpEntity(ContentTypes.`application/json`, platformUser.toJSON)
          complete(StatusCodes.OK, value)
        }
      }
    }
  }
}
