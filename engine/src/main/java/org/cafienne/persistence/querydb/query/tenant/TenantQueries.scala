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

package org.cafienne.persistence.querydb.query.tenant

import org.cafienne.actormodel.identity._
import org.cafienne.persistence.querydb.record.TenantRecord

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.concurrent.Future

trait TenantQueries {
  def getTenant(tenantId: String): Future[TenantRecord]

  def getTenantUser(user: UserIdentity, tenant: String): Future[TenantUser]

  def getPlatformUser(userId: String): Future[PlatformUser]

  def determineOriginOfUsers(users: Seq[String], tenant: String): Future[Seq[(String, Origin)]]

  def getTenantUsers(tenantUser: TenantUser): Future[Seq[TenantUser]]

  def getDisabledTenantUserAccounts(tenantUser: TenantUser): Future[Seq[TenantUser]]

  def getTenantUser(tenantUser: TenantUser, userId: String): Future[TenantUser]

  def getTenantGroupsUsage(user: TenantUser, tenant: String): Future[Map[String, mutable.HashMap[String, ListBuffer[String]]]]
}
