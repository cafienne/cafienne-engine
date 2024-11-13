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

package com.casefabric.infrastructure.cqrs.batch.public_events

import com.casefabric.json.{CaseFabricJson, Value, ValueMap}

import java.time.Instant

case class PublicEventWrapper(timestamp: Instant, sequenceNr: Long = 0, content: CaseFabricPublicEventContent) extends CaseFabricJson {

  lazy val manifest: String   = content.getClass.getSimpleName

  override def toString: String = {
    toValue.toString
  }

  override def toValue: Value[_] = {
    // Metadata carries manifest and timestamp.
    val metadata = new ValueMap("manifest", manifest, "timestamp", timestamp)
    // always enrich content with event type as well
    val contentJson: ValueMap = content.toValue.asMap().plus("casefabric-event-type", manifest)

    new ValueMap("metadata", metadata, "content", contentJson)
  }
}
