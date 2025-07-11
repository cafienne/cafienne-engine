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

package org.cafienne.service.storage.restore.event

import org.cafienne.infrastructure.serialization.Manifest
import org.cafienne.json.ValueMap
import org.cafienne.service.storage.actormodel.ActorMetadata
import org.cafienne.service.storage.actormodel.message.StorageActionCompleted

@Manifest
case class RestoreCompleted(metadata: ActorMetadata, override val optionalJson: Option[ValueMap] = None) extends RestoreEvent with StorageActionCompleted

object RestoreCompleted {
  def deserialize(json: ValueMap): RestoreCompleted = {
    RestoreCompleted(ActorMetadata.deserializeMetadata(json), Some(json))
  }
}
