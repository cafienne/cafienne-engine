/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.actormodel.tagging

import akka.persistence.journal.{Tagged, WriteEventAdapter}
import com.typesafe.scalalogging.LazyLogging
import org.cafienne.actormodel.event.ModelEvent

class CaseTaggingEventAdapter extends WriteEventAdapter with LazyLogging {
  logger.warn("You can safely remove the CaseTaggingEventAdapter properties in the read journal configuration")

  override def manifest(event: Any): String = ""

  override def toJournal(event: Any): Any = event match {
    case event: ModelEvent =>
      import scala.jdk.CollectionConverters.SetHasAsScala
      Tagged(event, event.tags().asScala.toSet)
    case _ => event
  }
}
