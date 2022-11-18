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
