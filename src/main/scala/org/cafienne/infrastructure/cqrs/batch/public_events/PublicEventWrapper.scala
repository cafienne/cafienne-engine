/*
 * Copyright 2014 - 2022 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.cafienne.infrastructure.cqrs.batch.public_events

import org.cafienne.json.{CafienneJson, Value, ValueMap}

import java.time.Instant

case class PublicEventWrapper(timestamp: Instant, sequenceNr: Long = 0, content: CafiennePublicEventContent) extends CafienneJson {

  lazy val manifest: String   = content.getClass.getSimpleName

  override def toString: String = {
    toValue.toString
  }

  override def toValue: Value[_] = {
    // Metadata carries manifest and timestamp.
    val metadata = new ValueMap("manifest", manifest, "timestamp", timestamp)
    // always enrich content with event type as well
    val contentJson: ValueMap = content.toValue.asMap().plus("cafienne-event-type", manifest)

    new ValueMap("metadata", metadata, "content", contentJson)
  }
}
