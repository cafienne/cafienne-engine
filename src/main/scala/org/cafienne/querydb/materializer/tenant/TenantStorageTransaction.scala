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
import org.cafienne.actormodel.identity.TenantUser
import org.cafienne.cmmn.actorapi.command.platform.NewUserInformation
import org.cafienne.infrastructure.cqrs.offset.OffsetRecord
import org.cafienne.querydb.materializer.QueryDBTransaction
import org.cafienne.querydb.record.{TenantRecord, UserRoleKey, UserRoleRecord}

import scala.concurrent.Future

trait TenantStorageTransaction extends QueryDBTransaction {
  def upsert(record: TenantRecord): Unit

  def upsert(record: UserRoleRecord): Unit

  def delete(record: UserRoleRecord): Unit

  def deleteTenantUser(user: TenantUser): Unit

  def getUserRole(key: UserRoleKey): Future[Option[UserRoleRecord]]

  def updateTenantUserInformation(tenant: String, info: Seq[NewUserInformation], offset: OffsetRecord): Future[Done]
}
