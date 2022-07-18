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

class PublicEventWrapper(val timestamp: Instant, val content: CafiennePublicEventContent) extends CafienneJson {

  lazy val manifest: String   = content.getClass.getSimpleName

  override def toString: String = {
    toValue.toString
  }

  override def toValue: Value[_] = {
    val contentJson: ValueMap = content.toValue.asMap()
    contentJson.plus("cafienne-event-type", manifest)
    new ValueMap("metadata", metadata, "content", contentJson)
  }

  def metadata: ValueMap = {
    new ValueMap("manifest", manifest, "timestamp", timestamp)
  }
}
