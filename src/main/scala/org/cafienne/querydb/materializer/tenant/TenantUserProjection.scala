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

package org.cafienne.querydb.materializer.tenant

import org.apache.pekko.Done
import com.typesafe.scalalogging.LazyLogging
import org.cafienne.actormodel.identity.TenantUser
import org.cafienne.querydb.record.{UserRoleKey, UserRoleRecord}
import org.cafienne.tenant.actorapi.event.deprecated._
import org.cafienne.tenant.actorapi.event.user.{TenantMemberEvent, TenantUserAdded, TenantUserChanged, TenantUserRemoved}

import scala.concurrent.{ExecutionContext, Future}

class TenantUserProjection(override val batch: TenantEventBatch)(implicit val executionContext: ExecutionContext) extends  TenantEventMaterializer with LazyLogging {
  private val deprecatedUserEventRecords = scala.collection.mutable.HashMap[UserRoleKey, UserRoleRecord]()

  private val userRolesAdded = scala.collection.mutable.ListBuffer[UserRoleRecord]()
  private val userRolesRemoved = scala.collection.mutable.ListBuffer[UserRoleRecord]()
  private val usersRemoved = scala.collection.mutable.Set[TenantUser]()

  def handleDeprecatedUserEvent(event: DeprecatedTenantUserEvent): Future[Done] = {
    //    println("Clearing user " + event.userId +" from user cache")
    val key = UserRoleKey(event)
    getUserRoleRecord(key).map(user => {
      event match {
        case t: TenantUserCreated => deprecatedUserEventRecords.put(key, user.copy(name = t.name, email = t.email, enabled = true))
        case t: TenantUserUpdated => deprecatedUserEventRecords.put(key, user.copy(name = t.name, email = t.email, enabled = true))
        case _: TenantUserRoleAdded => deprecatedUserEventRecords.put(key, user.copy(enabled = true))
        case _: TenantUserRoleRemoved => deprecatedUserEventRecords.put(key, user.copy(enabled = false))
        case _: OwnerAdded => deprecatedUserEventRecords.put(key, user.copy(isOwner = true))
        case _: OwnerRemoved => deprecatedUserEventRecords.put(key, user.copy(isOwner = false))
        case _: TenantUserDisabled => deprecatedUserEventRecords.put(key, user.copy(enabled = false))
        case _: TenantUserEnabled => deprecatedUserEventRecords.put(key, user.copy(enabled = true))
        case _ => // Others not known currently
      }
      Done
    })
  }

  private def getUserRoleRecord(key: UserRoleKey): Future[UserRoleRecord] = {
    deprecatedUserEventRecords.get(key) match {
      case Some(value) =>
        logger.debug(s"Retrieved user_role[$key] from current transaction cache")
        Future.successful(value)
      case None =>
        logger.debug(s"Retrieving user_role[$key] from database")
        dBTransaction.getUserRole(key).map {
          case Some(value) => value
          case None => UserRoleRecord(key.userId, key.tenant, key.role_name, "", "", isOwner = false, enabled = true)
        }
    }
  }

  def handleUserEvent(event: TenantMemberEvent): Future[Done] = {
    val user = event.member
    event match {
      case _: TenantUserRemoved => usersRemoved += user
      case other =>
        userRolesAdded += UserRoleRecord(userId = user.id, tenant = user.tenant, role_name = "", name = user.name, email = user.email, isOwner = user.isOwner, enabled = user.enabled)
        other match {
          case _: TenantUserAdded =>
            user.roles.foreach(role => userRolesAdded += UserRoleRecord(userId = user.id, tenant = user.tenant, role_name = role, name = "", "", isOwner = false, enabled = true))
          case event: TenantUserChanged =>
            user.roles.foreach(role => userRolesAdded += UserRoleRecord(userId = user.id, tenant = user.tenant, role_name = role, name = "", "", isOwner = false, enabled = true))
            event.rolesRemoved.forEach(role => userRolesRemoved += UserRoleRecord(userId = user.id, tenant = user.tenant, role_name = role, name = "", "", isOwner = false, enabled = true))
          case _ => // Ignore others (there aren't any)
        }
    }
    Future.successful(Done)
  }

  def affectedUserIds: Set[String] = (deprecatedUserEventRecords.values ++ userRolesAdded ++ userRolesRemoved).map(_.userId).toSet ++ usersRemoved.map(_.id)

  def prepareCommit(): Unit = {
    this.deprecatedUserEventRecords.values.foreach(instance => dBTransaction.upsert(instance))
    this.userRolesAdded.foreach(dBTransaction.upsert)
    this.userRolesRemoved.foreach(dBTransaction.delete)
    this.usersRemoved.foreach(dBTransaction.deleteTenantUser)
  }
}
