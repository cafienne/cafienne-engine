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

package org.cafienne.persistence.infrastructure.jdbc.query

case class Sort(on: Option[String], direction: Option[String] = Some("desc")) {
  lazy val ascending = direction.fold(false)(d => if (d matches "(?i)asc")  true else false)
}

object Sort {
  def NoSort: Sort = Sort(None, None)

  def asc(field: String) = Sort(Some(field), Some("asc"))

  def on(field: String) = Sort(Some(field))

  def withDefault(on: Option[String], direction: Option[String], defaultOnField: String) = Sort(Some(on.getOrElse(defaultOnField)), direction)
}
