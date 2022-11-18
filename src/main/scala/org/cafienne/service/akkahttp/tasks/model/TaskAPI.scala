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

package org.cafienne.service.akkahttp.tasks.model

import io.swagger.v3.oas.annotations.media.Schema
import org.cafienne.infrastructure.akkahttp.EntityReader.{EntityReader, entityReader}

import scala.annotation.meta.field

object TaskAPI {

  implicit val assigneeReader: EntityReader[Assignee] = entityReader[Assignee]

  @Schema(description = "Assign a task to someone")
  case class Assignee(@(Schema@field)(description = "Assignee", required = true, implementation = classOf[String]) assignee: String)

  object Examples {
    @Schema(description = "Output parameters example json")
    case class TaskOutputFormat(output1: String, output2: Object, output3: List[String])
  }
}
