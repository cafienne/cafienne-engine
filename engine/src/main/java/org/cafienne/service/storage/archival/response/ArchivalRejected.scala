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

package org.cafienne.service.storage.archival.response

import org.cafienne.infrastructure.serialization.{Fields, Manifest}
import org.cafienne.json.ValueMap
import org.cafienne.service.storage.actormodel.ActorMetadata
import org.cafienne.service.storage.actormodel.message.StorageActionRejected

@Manifest
case class ArchivalRejected(metadata: ActorMetadata, msg: String, override val optionalJson: Option[ValueMap] = None) extends StorageActionRejected

object ArchivalRejected {
  def deserialize(json: ValueMap): ArchivalRejected = {
    ArchivalRejected(ActorMetadata.deserializeMetadata(json), json.readString(Fields.message, ""), Some(json))
  }
}
