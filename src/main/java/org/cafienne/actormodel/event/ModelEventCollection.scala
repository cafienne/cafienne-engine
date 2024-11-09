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

package org.cafienne.actormodel.event

import scala.collection.mutable.ListBuffer

trait ModelEventCollection {
  val events: ListBuffer[ModelEvent] = ListBuffer()

  def eventsOfType[ME <: ModelEvent](clazz: Class[ME]): Seq[ME] = events.filter(event => clazz.isAssignableFrom(event.getClass)).map(_.asInstanceOf[ME]).toSeq

  def optionalEvent[ME <: ModelEvent](clazz: Class[ME]): Option[ME] = eventsOfType(clazz).headOption

  def getEvent[ME <: ModelEvent](clazz: Class[ME]): ME = optionalEvent(clazz).get
}
