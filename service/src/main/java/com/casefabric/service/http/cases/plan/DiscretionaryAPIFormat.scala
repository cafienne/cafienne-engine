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

package com.casefabric.service.http.cases.plan

import io.swagger.v3.oas.annotations.media.Schema
import com.casefabric.service.infrastructure.payload.EntityReader.{EntityReader, entityReader}

import scala.annotation.meta.field

object DiscretionaryAPIFormat {
  implicit val discretionaryItemReader: EntityReader[PlanDiscretionaryItem] = entityReader[PlanDiscretionaryItem]

  @Schema(description = "Add a discretionary item to the plan")
  case class PlanDiscretionaryItem(
                                    @(Schema@field)(
                                      description = "Name of the item to be added",
                                      required = true,
                                      example = "Task such and so",
                                      implementation = classOf[String])
                                    name: String,
                                    @(Schema@field)(
                                      description = "Identifier of the item in the case definition",
                                      required = true,
                                      example = "Identifier of the item in the case definition",
                                      implementation = classOf[String])
                                    definitionId: String,
                                    @(Schema@field)(
                                      description = "Identifier of the parent stage or human task to which the item belongs",
                                      required = true,
                                      example = "guid or so",
                                      implementation = classOf[String])
                                    parentId: String,
                                    @(Schema@field)(
                                      description = "Optional identifier for the newly added plan item. If omitted, it will be generated by the server",
                                      required = false,
                                      example = "Task such and so",
                                      implementation = classOf[String])
                                    planItemId: Option[String]
                                  )

  case class DiscretionaryItem(name: String, definitionId: String, `type`: String, parentName: String, parentType: String, parentId: String)

  case class DiscretionaryItemsList(caseInstanceId: String, discretionaryItems: Seq[DiscretionaryItem])

  case class PlannedDiscretionaryItem(planItemId: String)
}
