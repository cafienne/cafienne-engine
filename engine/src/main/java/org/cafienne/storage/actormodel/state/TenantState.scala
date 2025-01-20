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

package org.cafienne.storage.actormodel.state

import org.cafienne.storage.actormodel.ActorMetadata
import org.cafienne.storage.querydb.TenantStorage

trait TenantState extends QueryDBState {
  override val dbStorage: TenantStorage = new TenantStorage(actor.caseSystem.queryDB.writer)

  override def findCascadingChildren(): Seq[ActorMetadata] = {
    printLogMessage("Running tenant query on cases and groups")

    val storedCases = dbStorage.readCases(metadata.actorId)
    val storedGroups = dbStorage.readGroups(metadata.actorId)

    val cases = storedCases.map(id => metadata.caseMember(id))
    val groups = storedGroups.map(id => metadata.groupMember(id))
    printLogMessage(s"Found ${cases.length} cases and ${groups.length} groups")
    cases ++ groups
  }

  override def clearQueryData(): Unit = dbStorage.deleteTenant(actorId)
}
