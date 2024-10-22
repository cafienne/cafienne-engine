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

package org.cafienne.service.http.tasks

import io.swagger.v3.oas.annotations.media.Schema
import org.cafienne.infrastructure.http.EntityReader.{EntityReader, entityReader}

import java.time.Instant
import scala.annotation.meta.field

object TaskAPIFormat {

  implicit val assigneeReader: EntityReader[Assignee] = entityReader[Assignee]

  @Schema(description = "Assign a task to someone")
  case class Assignee(@(Schema@field)(description = "Assignee", required = true, implementation = classOf[String]) assignee: String)

  object Examples {
    @Schema(description = "Output parameters example json")
    case class TaskOutputFormat(output1: String, output2: Object, output3: List[String])
  }

  @Schema(description = "Task response format")
  case class TaskResponseFormat(@(Schema @field)(
                                  description = "Id of the task",
                                  example = "3cbb2a96_08b3_4c13_a87a_b3f073a74e32",
                                  implementation = classOf[String])
                                id: String,
                                @(Schema @field)(
                                  description = "Name of the task",
                                  example = "Name of the task",
                                  implementation = classOf[String])
                                taskName: String,
                                @(Schema @field)(
                                  description = "Current state of the task ('Null', 'Unassigned', 'Assigned', 'Delegated', 'Completed', 'Suspended' or 'Terminated')",
                                  example = "'Null', 'Unassigned', 'Assigned', 'Delegated', 'Completed', 'Suspended' or 'Terminated'",
                                  implementation = classOf[String])
                                taskState: String = "",
                                @(Schema @field)(
                                  description = "Identifier of the user to which the task is assigned, can be empty",
                                  example = "user-id",
                                  implementation = classOf[String])
                                assignee: String,
                                @(Schema @field)(
                                  description = "Task owner (deviates from assignee when the task is delegated)",
                                  example = "user-id",
                                  implementation = classOf[String])
                                owner: String,
                                @(Schema @field)(
                                  description = "Tenant to which the case that has the task belongs",
                                  example = "Identifier of the tenant",
                                  implementation = classOf[String])
                                tenant: String,
                                @(Schema @field)(
                                  description = "Id of the case to which the task belongs",
                                  example = "c7364fc1-939c-4a94-b8f9-39480d516848",
                                  implementation = classOf[String])
                                caseInstanceId: String,
                                @(Schema @field)(
                                  description = "Case role required to perform this task, can be empty",
                                  example = "Approver",
                                  implementation = classOf[String])
                                role: String,
                                @(Schema @field)(
                                  description = "The moment at which the plan item was modified",
                                  example = "2024-05-06T17:06:12.113204800Z",
                                  implementation = classOf[Instant])
                                lastModified: Instant,
                                @(Schema @field)(
                                  description = "Id of the user that did the last modification to the plan item",
                                  example = "user-id",
                                  implementation = classOf[String])
                                modifiedBy: String,
                                @(Schema @field)(
                                  description = "Optional due date of the task",
                                  example = "2024-05-06T17:06:12.113204800Z",
                                  implementation = classOf[Instant])
                                dueDate: Instant,
                                @(Schema @field)(
                                  description = "The moment at which the task was created",
                                  example = "2024-05-06T17:06:12.113204800Z",
                                  implementation = classOf[Instant])
                                createdOn: Instant,
                                @(Schema @field)(
                                  description = "Id of the user that created the task",
                                  example = "creating-user-id",
                                  implementation = classOf[String])
                                createdBy: String,
                                @(Schema @field)(
                                  description = "JSON document with the input of the task",
                                  example = "{}",
                                  implementation = classOf[Object])
                                input: Object,
                                @(Schema @field)(
                                  description = "JSON document with the current output of the task",
                                  example = "{}",
                                  implementation = classOf[Object])
                                output: Object,
                                @(Schema @field)(
                                  description = "String or JSON document with the task model. This can be a reference to a user interface.",
                                  example = "{}",
                                  implementation = classOf[Object])
                                taskModel: Object,
                               )
}
