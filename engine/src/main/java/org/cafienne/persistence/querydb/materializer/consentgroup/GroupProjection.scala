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

package org.cafienne.persistence.querydb.materializer.consentgroup

import com.typesafe.scalalogging.LazyLogging
import org.cafienne.userregistration.consentgroup.actorapi.event.ConsentGroupCreated
import org.cafienne.persistence.querydb.record.ConsentGroupRecord

class GroupProjection(override val batch: ConsentGroupEventBatch) extends ConsentGroupEventMaterializer with LazyLogging {
  def handleGroupEvent(event: ConsentGroupCreated): Unit = {
    val groupRecord = ConsentGroupRecord(id = event.getActorId, tenant = event.tenant)
    dBTransaction.upsert(groupRecord)
  }

  def prepareCommit(): Unit = {
    // Nothing to do here currently
  }
}
