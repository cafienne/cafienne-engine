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

package org.cafienne.service.http.cases

import io.swagger.v3.oas.annotations.media.Schema
import org.cafienne.infrastructure.http.EntityReader.{EntityReader, entityReader}
import org.cafienne.json.ValueMap
import org.cafienne.service.http.cases.plan.PlanItemAPIFormat.PlanItemResponseFormat
import org.cafienne.service.http.cases.team.CaseTeamAPI

import java.time.Instant
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

  @Schema(description = "Response when getting case instance information")
  case class CaseResponseFormat(@(Schema @field)(
                                  description = "Identifier of the case",
                                  example = "typically a guid with underscores ;)",
                                  implementation = classOf[String])
                                id: String,
                                @(Schema @field)(
                                  description = "Id of the tenant where the case resides",
                                  example = "world",
                                  implementation = classOf[String])
                                tenant: String,
                                @(Schema @field)(
                                  description = "Name of the case, taken from the definition",
                                  example = "Name of the case, taken from the definition",
                                  implementation = classOf[String])
                                caseName: String,
                                @(Schema @field)(
                                  description = "Deprecated field, contains same value as caseName",
                                  example = "Deprecated field, contains same value as caseName",
                                  implementation = classOf[String])
                                definition: String,
                                @(Schema @field)(
                                  description = "Deprecated field, contains same value as caseName",
                                  example = "Deprecated field, contains same value as caseName",
                                  implementation = classOf[String])
                                name: String,
                                @(Schema @field)(
                                  description = "Current state of the case (equal to the state of the CasePlan)",
                                  example = "Active, Suspended, Completed, Terminated, etc.",
                                  implementation = classOf[String])
                                state: String = "",
                                @(Schema @field)(
                                  description = "Deprecated field. Holds the number of plan items inside the case that are in state Failed.",
                                  example = "0",
                                  implementation = classOf[Integer])
                                failures: Int,
                                @(Schema @field)(
                                  description = "Identifier of parent case in which this case resides as a CaseTask. Empty when this is the top level case.",
                                  example = "c7364fc1_939c_4a94_b8f9_39480d516848",
                                  implementation = classOf[String])
                                parentCaseId: String,
                                @(Schema @field)(
                                  description = "Identifier of top level case in which this case resides. Same as id of the case when this is the top level case",
                                  example = "c7364fc1_939c_4a94_b8f9_39480d516848",
                                  implementation = classOf[String])
                                rootCaseId: String,
                                @(Schema @field)(
                                  description = "The moment at which the case was created",
                                  example = "2024-05-06T17:06:12.113204800Z",
                                  implementation = classOf[Instant])
                                createdOn: Instant,
                                @(Schema @field)(
                                  description = "Id of the user that created the case",
                                  example = "creating-user-id",
                                  implementation = classOf[String])
                                createdBy: String,
                                @(Schema @field)(
                                  description = "The moment of the last modification to the case",
                                  example = "2024-05-06T17:06:12.113204800Z",
                                  implementation = classOf[Instant])
                                lastModified: Instant,
                                @(Schema @field)(
                                  description = "Id of the user that did the last modification to the case",
                                  example = "user-id",
                                  implementation = classOf[String])
                                modifiedBy: String,
                                @(Schema @field)(
                                  description = "The case plan, as a list of plan items. Note: this is empty when retrieving a list of cases.")
                                planitems: Seq[PlanItemResponseFormat],
                                @(Schema @field)(
                                  description = "The team that has access to the case. Note: this is empty when retrieving a list of cases.",
                                  implementation = classOf[CaseTeamAPI.TeamFormat])
                                team: CaseTeamAPI.TeamFormat,
                                @(Schema @field)(
                                  description = "A JSON representation of the case file. Note: this is empty when retrieving a list of cases.",
                                  implementation = classOf[Object])
                                file: Object,
                                @(Schema @field)(
                                  description = "The list of key/value pairs belonging to this case. Such identifiers can be used as labels to group cases. Note: this is empty when retrieving a list of cases.")
                                identifiers: Seq[CaseIdentifierFormat],
                               )

  case class CaseIdentifierFormat(name: String, value: String)

  case class CaseSummaryResponseFormat(@(Schema @field)(
    description = "Identifier of the case",
    example = "typically a guid with underscores ;)",
    implementation = classOf[String])
                                id: String,
                                @(Schema @field)(
                                  description = "Id of the tenant where the case resides",
                                  example = "world",
                                  implementation = classOf[String])
                                tenant: String,
                                @(Schema @field)(
                                  description = "Name of the case, taken from the definition",
                                  example = "Name of the case, taken from the definition",
                                  implementation = classOf[String])
                                caseName: String,
                                @(Schema @field)(
                                  description = "Deprecated field, contains same value as caseName",
                                  example = "Deprecated field, contains same value as caseName",
                                  implementation = classOf[String])
                                definition: String,
                                @(Schema @field)(
                                  description = "Deprecated field, contains same value as caseName",
                                  example = "Deprecated field, contains same value as caseName",
                                  implementation = classOf[String])
                                name: String,
                                @(Schema @field)(
                                  description = "Current state of the case (equal to the state of the CasePlan)",
                                  example = "Active, Suspended, Completed, Terminated, etc.",
                                  implementation = classOf[String])
                                state: String = "",
                                @(Schema @field)(
                                  description = "Deprecated field. Holds the number of plan items inside the case that are in state Failed.",
                                  example = "0",
                                  implementation = classOf[Integer])
                                failures: Int,
                                @(Schema @field)(
                                  description = "Identifier of parent case in which this case resides as a CaseTask. Empty when this is the top level case.",
                                  example = "c7364fc1_939c_4a94_b8f9_39480d516848",
                                  implementation = classOf[String])
                                parentCaseId: String,
                                @(Schema @field)(
                                  description = "Identifier of top level case in which this case resides. Same as id of the case when this is the top level case",
                                  example = "c7364fc1_939c_4a94_b8f9_39480d516848",
                                  implementation = classOf[String])
                                rootCaseId: String,
                                @(Schema @field)(
                                  description = "The moment at which the case was created",
                                  example = "2024-05-06T17:06:12.113204800Z",
                                  implementation = classOf[Instant])
                                createdOn: Instant,
                                @(Schema @field)(
                                  description = "Id of the user that created the case",
                                  example = "creating-user-id",
                                  implementation = classOf[String])
                                createdBy: String,
                                @(Schema @field)(
                                  description = "The moment of the last modification to the case",
                                  example = "2024-05-06T17:06:12.113204800Z",
                                  implementation = classOf[Instant])
                                lastModified: Instant,
                                @(Schema @field)(
                                  description = "Id of the user that did the last modification to the case",
                                  example = "user-id",
                                  implementation = classOf[String])
                                modifiedBy: String,
                                @(Schema @field)(
                                  description = "The plan item list is not filled when retrieving a list of cases",
                                  example = "[]")
                                planitems: Seq[PlanItemResponseFormat],
                                @(Schema @field)(
                                  description = "The case team is not filled when retrieving a list of cases",
                                  example = "{}")
                                team: CaseTeamAPI.TeamFormat,
                                @(Schema @field)(
                                  description = "The case file is not filled when retrieving a list of cases")
                                file: Object,
                               )

  @Schema(description = "Response when getting case instance documentation", example = "<definitions xmlns=\"http://www.omg.org/spec/CMMN/20151109/MODEL\" xmlns:cafienne=\"org.cafienne\"><case id=\"caseDefinitionId\">The definitions tag is CMMN XSD compliant</case></definitions>")
  class CaseDefinitionFormat()
}
