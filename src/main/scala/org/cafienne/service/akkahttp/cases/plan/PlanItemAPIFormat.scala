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

package org.cafienne.service.akkahttp.cases.plan

import io.swagger.v3.oas.annotations.media.Schema

import java.time.Instant
import scala.annotation.meta.field

object PlanItemAPIFormat {
  @Schema(description = "Plan Item format")
  case class PlanItemResponseFormat(@(Schema @field)(
                                      description = "Id of the plan item",
                                      example = "3cbb2a96_08b3_4c13_a87a_b3f073a74e32",
                                      implementation = classOf[String])
                                    id: String,
                                    @(Schema @field)(
                                      description = "Id of the case to which the item belongs",
                                      example = "c7364fc1-939c-4a94-b8f9-39480d516848",
                                      implementation = classOf[String])
                                    caseInstanceId: String,
                                    @(Schema @field)(
                                      description = "Identifier of the item in the case definition",
                                      example = "Identifier of the item in the case definition",
                                      implementation = classOf[String])
                                    definitionId: String,
                                    @(Schema @field)(
                                      description = "Identifier of the stage in which the item is created; gives an empty string for the top level case plan",
                                      example = "3b59d0e6_1bec_4c71_9ee6_62276c6ee52d",
                                      implementation = classOf[String])
                                    stageId: String,
                                    @(Schema @field)(
                                      description = "Name of the plan item",
                                      example = "Name of the plan item",
                                      implementation = classOf[String])
                                    name: String,
                                    @(Schema @field)(
                                      description = "Repetition index of the plan item.",
                                      example = "0",
                                      implementation = classOf[Integer])
                                    index: Int,
                                    @(Schema @field)(
                                      description = "Current state of the plan item ('Available', 'Active', etc.)",
                                      example = "Available, Active, Completed, Terminated, etc.",
                                      implementation = classOf[String])
                                    currentState: String = "",
                                    @(Schema @field)(
                                      description = "Previous state the plan item was in ('Available', 'Active', etc.)",
                                      example = "Available, Active, Completed, Terminated, etc.",
                                      implementation = classOf[String])
                                    historyState: String = "",
                                    @(Schema @field)(
                                      description = "Indication whether the plan item is mandatory or not for completion of the stage it resides in",
                                      implementation = classOf[Boolean])
                                    isRequired: Boolean = false,
                                    @(Schema @field)(
                                      description = "Indication whether the plan item may have a successor or not",
                                      implementation = classOf[Boolean])
                                    isRepeating: Boolean = false,
                                    @(Schema @field)(
                                      description = "Type of item (HumanTask, Milestone, Stage, etc.)",
                                      example = "HumanTask, Milestone, Stage, etc.",
                                      implementation = classOf[String])
                                    `type`: String,
                                    @(Schema @field)(
                                      description = "The last transition that the plan item had (Create, Complete, Terminate, etc.)",
                                      example = "Create, Complete, Terminate, etc.",
                                      implementation = classOf[String])
                                    transition: String = "",
                                    @(Schema @field)(
                                      description = "The moment at which the plan item was modified",
                                      example = "2024-05-06T17:06:12.113204800Z",
                                      implementation = classOf[Instant])
                                    lastModified: Instant,
                                    @(Schema @field)(
                                      description = "Id of the user that did the last modification to the plan item",
                                      example = "user-id",
                                      implementation = classOf[String])
                                    modifiedBy: String
                                   )
}
