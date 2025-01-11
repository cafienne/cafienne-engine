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

import com.typesafe.scalalogging.LazyLogging
import org.cafienne.querydb.record.TenantRecord
import org.cafienne.tenant.actorapi.event.platform.{PlatformEvent, TenantCreated, TenantDisabled, TenantEnabled}

class TenantProjection(override val batch: TenantEventBatch) extends TenantEventMaterializer with LazyLogging {
  private val tenants = scala.collection.mutable.HashMap[String, TenantRecord]()

  def handlePlatformEvent(event: PlatformEvent): Unit = {
    event match {
      case newTenant: TenantCreated => tenants.put(newTenant.tenantName, TenantRecord(newTenant.tenantName()))
      case disabledTenant: TenantDisabled => tenants.put(disabledTenant.tenantName(), TenantRecord(disabledTenant.tenantName(), enabled = false))
      case enabledTenant: TenantEnabled => tenants.put(enabledTenant.tenantName(), TenantRecord(enabledTenant.tenantName(), enabled = true))
      case _ => () // Ignore other events
    }
  }

  def prepareCommit(): Unit = {
    this.tenants.values.foreach(instance => dBTransaction.upsert(instance))
  }
}
