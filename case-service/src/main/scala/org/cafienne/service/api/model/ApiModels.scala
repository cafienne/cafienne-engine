/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.service.api.model

import java.time.Instant

import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.media.ArraySchema
import org.cafienne.akka.actor.serialization.json.ValueMap
import org.cafienne.cmmn.actorapi.command.team.CaseTeam

import scala.annotation.meta.field

final object Examples {
  @Schema(description = "Input parameters example json")
  case class InputParametersFormat(input1: String, input2: Object)

  @Schema(description = "Output parameters example json")
  case class OutputParametersFormat(output1: String, output2: Object, output3: List[String])

  @Schema(description = "Example case team")
  case class StartCaseTeamFormat(
                                  @(ArraySchema @field)(schema = new Schema(
                                    description = "If members is left empty, only current user will be added to the case team",
                                    required = true,
                                    implementation = classOf[StartCaseTeamMemberFormat]))
                                  members: Array[StartCaseTeamMemberFormat]
                                )

  @Schema(description = "Example case team member")
  case class StartCaseTeamMemberFormat(
                     @(Schema @field)(
                       description = "Identification of the team member (either user id or tenant role name)",
                       example = "Identification of the team member (either user id or tenant role name)",
                       required = true,
                       implementation = classOf[String])
                     memberId: String,
                     @(Schema @field)(
                       description = "Type of member, either 'user' or 'role'. If a member is of type 'role', then all tenant users with that role belong to the case team",
                       required = false,
                       implementation = classOf[String],
                       example="This field must contain either 'user' or 'role'. Defaults to 'user'. If 'role' is given, the memberId must contain a tenant role name, otherwise it must be a tenant user id",
                       allowableValues = Array("user", "role"))
                     memberType: String,
                     @(Schema @field)(
                       description = "Whether the member is owner to the case",
                       required = false,
                       implementation = classOf[Boolean],
                       example = "false or true")
                     isOwner: Boolean = false,
                     @(ArraySchema @field)(schema = new Schema(
                       description = "Zero or more roles that the member has in the case team. An empty set means that the member has no roles, but is still part of the team",
                       required = true,
                       implementation = classOf[NewCaseTeamRolesFormat],
                       example = "[\"Employee\",  \"Customer\", \"Supplier\"]"))
                     caseRoles: Array[String]
                   )

  @Schema(description = "Example case team member")
  case class PutCaseTeamMemberFormat(
                                  @(Schema @field)(
                                    description = "Identification of the team member (either user id or tenant role name)",
                                    example = "Identification of the team member (either user id or tenant role name)",
                                    required = true,
                                    implementation = classOf[String])
                                  memberId: String,
                                  @(Schema @field)(
                                    description = "Type of member, either 'user' or 'role'. If a member is of type 'role', then all tenant users with that role belong to the case team",
                                    required = false,
                                    implementation = classOf[String],
                                    example="This field must contain either 'user' or 'role'. Defaults to 'user'. If 'role' is given, the memberId must contain a tenant role name, otherwise it must be a tenant user id",
                                    allowableValues = Array("user", "role"))
                                  memberType: String,
                                  @(Schema @field)(
                                    description = "Optional field to fill when ownership needs to change for the member",
                                    required = false,
                                    implementation = classOf[Option[Boolean]],
                                    example = "Optional field to fill when ownership needs to change for the member; passing true will add ownership; passing false removes; not passing does not change existing ownership rights")
                                  isOwner: Option[Boolean] = None,
                                  @(ArraySchema @field)(schema = new Schema(
                                    description = "Zero or more case roles that will be added to the member",
                                    required = true,
                                    implementation = classOf[NewCaseTeamRolesFormat],
                                    example = "Zero or more case roles that will be added to the member"))
                                  caseRoles: Array[String],
                                  @(ArraySchema @field)(schema = new Schema(
                                    description = "Zero or more case roles that need to be removed from the member",
                                    required = true,
                                    implementation = classOf[RemoveCaseTeamRolesFormat],
                                    example = "Zero or more case roles that need to be removed from the member"))
                                  removeRoles: Array[String]
                                )
  case class NewCaseTeamRolesFormat()
  case class RemoveCaseTeamRolesFormat()

  @Schema(description = "Example case team member")
  case class CaseTeamMemberResponseFormat(
                                @(Schema @field)(
                                  description = "Identification of the team member (either user id or tenant role name)",
                                  example = "Identification of the team member (either user id or tenant role name)",
                                  implementation = classOf[String])
                                memberId: String,
                                @(Schema @field)(
                                  description = "Type of member, either 'user' or 'role'. If a member is of type 'role', then all tenant users with that role belong to the case team",
                                  implementation = classOf[String],
                                  example = "Type of member, either 'user' or 'role'. If a member is of type 'role', then all tenant users with that role belong to the case team",
                                  allowableValues = Array("user", "role"))
                                memberType: String,
                                @(Schema @field)(
                                  description = "True if the member is a Case Owner; Case Owners are authorized to manage the case and the case team",
                                  implementation = classOf[Option[Boolean]],
                                  example = "True if the member is a Case Owner; Case Owners are authorized to manage the case and the case team")
                                isOwner: Boolean,
                                @(ArraySchema @field)(schema = new Schema(
                                  description = "Zero or more case roles that will be added to the member",
                                  implementation = classOf[NewCaseTeamRolesFormat],
                                  example = "Zero or more case roles that will be added to the member"))
                                caseRoles: Array[String],
                              )

  @Schema(description = "Example case team")
  case class CaseTeamResponseFormat(
                               @(ArraySchema @field)(schema = new Schema(
                             description = "Names of roles as defined in the case definition",
                             example = "Names of roles as defined in the case definition",
                             implementation = classOf[CaseDefinedRolesFormat]))
                           caseRoles: Array[String],
                               @(ArraySchema @field)(schema = new Schema(
                              description = "Members of the case team",
                              required = true,
                              implementation = classOf[CaseTeamMemberResponseFormat]))
                            members: Array[CaseTeamMemberResponseFormat],
                               @(ArraySchema @field)(schema = new Schema(
                             description = "Names of defined roles that are not assigned to any of the team members",
                             example = "Names of defined roles that are not assigned to any of the team members",
                             implementation = classOf[UnassignedRolesFormat]))
                           unassignedRoles: Array[String]
                             )

  case class CaseDefinedRolesFormat()
  case class UnassignedRolesFormat()
  case class CaseTeamRolesFormat()
}

@Schema(description = "Start the execution of a new case")
case class StartCaseFormat(
                      @(Schema @field)(
                        description = "Definition of the case to be started",
                        required = true,
                        example = "Depending on the internally configured DefinitionProvider this can be a file name or the case model itself.",
                        implementation = classOf[String])
                      definition: String = "", // by default an empty string to avoid nullpointers down the line
                      @(Schema @field)(
                        description = "Input parameters that will be passed to the started case",
                        required = false,
                        implementation = classOf[Examples.InputParametersFormat])
                      inputs: ValueMap,
                      @(Schema @field)(
                        description = "The team that will be connected to the execution of this case",
                        required = false,
                        implementation = classOf[Examples.StartCaseTeamFormat])
                      caseTeam: Option[BackwardCompatibleTeamFormat],
                      @(Schema @field)(description = "Tenant in which to create the case. If empty, default tenant as configured is taken.", required = false, implementation = classOf[Option[String]], example = "Will be taken from settings if omitted or empty")
                      tenant: Option[String],
                      @(Schema @field)(description = "Unique identifier to be used for this case. When there is no identifier given, a UUID will be generated", required = false, example = "Will be generated if omitted or empty")
                      caseInstanceId: Option[String],
                      @(Schema @field)(description = "Indicator to start the case in debug mode", required = false, implementation = classOf[Boolean], example = "false")
                      debug: Option[Boolean])

case class BackwardCompatibleTeamFormat(members: Seq[BackwardCompatibleTeamMemberFormat] = Seq())

case class BackwardCompatibleTeamMemberFormat(user: Option[String], // Old property, to be ccompatiblty
                                              roles: Option[Seq[String]], // Old property, just keep it here to remain compatible
                                              // New structure below
                                              memberId: Option[String],
                                              memberType: Option[String],
                                              removeRoles: Option[Seq[String]],
                                              caseRoles: Option[Seq[String]],
                                              isOwner: Option[Boolean])

// CaseFileItem
case class CaseFileItemFormat(id: String,
                              name: String,
                              caseInstanceId: String,
                              properties: Option[List[PropertyFormat]],
                              parentCaseFileItemId: String,
                              content: String,
                              mimetype: String,
                              transition: String,
                              lastModified: Instant)


//base types
// Property of a caseFileItem
case class PropertyFormat(name: String, `type`: String, value: String)

case class BusinessIdentifierFormat(caseInstanceId: String, tenant: String, name: String, value: String)