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

package org.cafienne.querydb.query.filter

case class TaskFilter(tenant: Option[String] = None,
                      identifiers: Option[String] = None,
                      caseName: Option[String] = None,
                      taskName: Option[String] = None,
                      taskState: Option[String] = None,
                      assignee: Option[String] = None,
                      owner: Option[String] = None,
                      dueOn: Option[String] = None,
                      dueBefore: Option[String] = None,
                      dueAfter: Option[String] = None,
                      timeZone: Option[String] = None)
