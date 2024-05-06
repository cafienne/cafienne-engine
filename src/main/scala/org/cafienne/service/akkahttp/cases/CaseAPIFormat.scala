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

package org.cafienne.service.akkahttp.cases

import io.swagger.v3.oas.annotations.media.Schema
import org.cafienne.infrastructure.akkahttp.EntityReader._
import org.cafienne.json.ValueMap
import org.cafienne.service.akkahttp.cases.team.CaseTeamAPI

import scala.annotation.meta.field

object CaseAPIFormat {
  implicit val startCaseReader: EntityReader[StartCaseFormat] = entityReader[StartCaseFormat]

  @Schema(description = "Start the execution of a new case")
  case class StartCaseFormat(
                              @(Schema@field)(
                                description = "Definition of the case to be started",
                                required = true,
                                example = "Depending on the internally configured DefinitionProvider this can be a file name or the case model itself.",
                                implementation = classOf[String])
                              definition: String = "", // by default an empty string to avoid nullpointers down the line
                              @(Schema@field)(
                                description = "Input parameters that will be passed to the started case",
                                required = false,
                                implementation = classOf[Examples.InputParametersFormat])
                              inputs: ValueMap,
                              @(Schema@field)(
                                description = "The team that will be connected to the execution of this case",
                                required = false,
                                implementation = classOf[CaseTeamAPI.TeamFormat])
                              caseTeam: CaseTeamAPI.Compatible.TeamFormat = CaseTeamAPI.Compatible.TeamFormat(),
                              @(Schema@field)(description = "Tenant in which to create the case. If empty, default tenant as configured is taken.", required = false, implementation = classOf[Option[String]], example = "Will be taken from settings if omitted or empty")
                              tenant: Option[String],
                              @(Schema@field)(description = "Unique identifier to be used for this case. When there is no identifier given, a UUID will be generated", required = false, example = "Will be generated if omitted or empty")
                              caseInstanceId: Option[String],
                              @(Schema@field)(description = "Indicator to start the case in debug mode", required = false, implementation = classOf[Boolean], example = "false")
                              debug: Option[Boolean])

  @Schema(description = "Response upon successful case creation")
  case class StartCaseResponse(
                                @(Schema@field)(
                                  description = "Identifier of the case that was created (this may have been generated)",
                                  example = "3e074f00_bb0d_4858_af0f_2424f8a2043a",
                                  implementation = classOf[String])
                                caseInstanceId: String,
                                @(Schema@field)(
                                  description = "Name of the case (taken from inside the definition)",
                                  example = "HelloWorld",
                                  implementation = classOf[String])
                                name: String)

  object Examples {
    @Schema(description = "Input parameters example json")
    case class InputParametersFormat(input1: String, input2: Object)
  }
}
