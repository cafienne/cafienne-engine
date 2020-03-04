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
    validUser { user =>
      path(Segment / "users") { tenant =>
        import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
        import spray.json.DefaultJsonProtocol._
        implicit val format = jsonFormat4(TenantAPI.User)
        entity(as[TenantAPI.User]) {
          newUser =>
//            System.err.println("New registration: " + newRegistration)
            val tenantOwner = user.getTenantUser(tenant)
            val roles = newUser.roles.asJava
            val name = newUser.name.getOrElse("")
            val email = newUser.email.getOrElse("")
            askTenant(new AddTenantUser(tenantOwner, tenant, newUser.userId, roles, name, email))
        }
      }
    }
  }

  def disableTenantUser = put {
    validUser { tenantOwner =>
      path(Segment / "users" / Segment / "disable") { (tenant, userId) =>
//        System.err.println("Disabling user " + userId + " in tenant " + tenant)
        val user = tenantOwner.getTenantUser(tenant)
        askTenant(new DisableTenantUser(user, tenant, userId))
      }
    }
  }

  def enableTenantUser = put {
    validUser { tenantOwner =>
      path(Segment / "users" / Segment / "enable") { (tenant, userId) =>
//        System.err.println("Enabling user " + userId + " in tenant " + tenant)
        val user = tenantOwner.getTenantUser(tenant)
        askTenant(new EnableTenantUser(user, tenant, userId))
      }
    }
  }

  def addTenantUserRoles = put {
    validUser { tenantOwner =>
      path(Segment / "users" / Segment / "roles" / Segment) { (tenant, userId, role) =>
//            System.err.println("New roles for user " + userId + " in tenant " + tenant + ": " + roles)
        val user = tenantOwner.getTenantUser(tenant)
        askTenant(new AddTenantUserRoles(user, tenant, userId, role))
      }
    }
  }

  def removeTenantUserRole = delete {
    validUser { user =>
      path(Segment / "users" / Segment / "roles" / Segment) { (tenant, userId, role) =>
//            System.err.println("Remove role for user " + userId + " in tenant " + tenant + ": " + role)
        val tenantOwner = user.getTenantUser(tenant)
        askTenant(new RemoveTenantUserRole(tenantOwner, tenant, userId, role))
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
        validUser { user =>
          val value = HttpEntity(ContentTypes.`application/json`, user.toJSON)
          complete(StatusCodes.OK, value)
        }
      }
    }
  }
}
