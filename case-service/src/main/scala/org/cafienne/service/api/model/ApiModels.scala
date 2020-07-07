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
import org.cafienne.cmmn.akka.command.team.CaseTeam
import org.cafienne.cmmn.instance.casefile.ValueMap

import scala.annotation.meta.field

final object Examples {
  @Schema(description = "Input parameters example json")
  case class InputParameters(input1: String, input2: Object)

  @Schema(description = "Output parameters example json")
  case class OutputParameters(output1: String, output2: Object, output3: List[String])

  @Schema(description = "Example case team")
  case class StartCaseTeam(
                                  @(ArraySchema @field)(schema = new Schema(
                                    description = "If members is left empty, only current user will be added to the case team",
                                    required = true,
                                    implementation = classOf[StartCaseTeamMember]))
                                  members: Array[StartCaseTeamMember]
                                )

  @Schema(description = "Example case team member")
  case class StartCaseTeamMember(
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
                       implementation = classOf[NewCaseTeamRoles],
                       example = "[\"Employee\",  \"Customer\", \"Supplier\"]"))
                     caseRoles: Array[String]
                   )

  @Schema(description = "Example case team member")
  case class PutCaseTeamMember(
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
                                    implementation = classOf[NewCaseTeamRoles],
                                    example = "Zero or more case roles that will be added to the member"))
                                  caseRoles: Array[String],
                                  @(ArraySchema @field)(schema = new Schema(
                                    description = "Zero or more case roles that need to be removed from the member",
                                    required = true,
                                    implementation = classOf[RemoveCaseTeamRoles],
                                    example = "Zero or more case roles that need to be removed from the member"))
                                  removeRoles: Array[String]
                                )
  case class NewCaseTeamRoles()
  case class RemoveCaseTeamRoles()

  @Schema(description = "Example case team member")
  case class CaseTeamMemberResponse(
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
                                  implementation = classOf[NewCaseTeamRoles],
                                  example = "Zero or more case roles that will be added to the member"))
                                caseRoles: Array[String],
                              )

  @Schema(description = "Example case team")
  case class CaseTeamResponse(
                           @(ArraySchema @field)(schema = new Schema(
                             description = "Names of roles as defined in the case definition",
                             example = "Names of roles as defined in the case definition",
                             implementation = classOf[CaseDefinedRoles]))
                           caseRoles: Array[String],
                            @(ArraySchema @field)(schema = new Schema(
                              description = "Members of the case team",
                              required = true,
                              implementation = classOf[CaseTeamMemberResponse]))
                            members: Array[CaseTeamMemberResponse],
                           @(ArraySchema @field)(schema = new Schema(
                             description = "Names of defined roles that are not assigned to any of the team members",
                             example = "Names of defined roles that are not assigned to any of the team members",
                             implementation = classOf[UnassignedRoles]))
                           unassignedRoles: Array[String]
                             )

  case class CaseDefinedRoles()
  case class UnassignedRoles()
  case class CaseTeamRoles()
}

@Schema(description = "Start the execution of a new case")
case class StartCaseAPI(
                      @(Schema @field)(
                        description = "Definition of the case to be started",
                        required = true,
                        example = "Depending on the internally configured DefinitionProvider this can be a file name or the case model itself.",
                        implementation = classOf[String])
                      definition: String,
                      @(Schema @field)(
                        description = "Input parameters that will be passed to the started case",
                        required = false,
                        implementation = classOf[Examples.InputParameters])
                      inputs: ValueMap,
                      @(Schema @field)(
                          description = "The team that will be connected to the execution of this case",
                          required = false,
                          implementation = classOf[Examples.StartCaseTeam])
                      caseTeam: Option[BackwardCompatibleTeam],
                      @(Schema @field)(description = "Tenant in which to create the case. If empty, default tenant as configured is taken.", required = false, implementation = classOf[Option[String]], example = "Will be taken from settings if omitted or empty")
                      tenant: Option[String],
                      @(Schema @field)(description = "Unique identifier to be used for this case. When there is no identifier given, a UUID will be generated", required = false, example = "Will be generated if omitted or empty")
                      caseInstanceId: Option[String],
                      @(Schema @field)(description = "Indicator to start the case in debug mode", required = false, implementation = classOf[Boolean], example = "false")
                      debug: Option[Boolean])

case class BackwardCompatibleTeam(members: Seq[BackwardCompatibleTeamMember] = Seq())

case class BackwardCompatibleTeamMember(user: Option[String], // Old property, to be ccompatiblty
                                        roles: Option[Seq[String]], // Old property, just keep it here to remain compatible
                                       // New structure below
                                        memberId: Option[String],
                                        memberType: Option[String],
                                        removeRoles: Option[Seq[String]],
                                        caseRoles: Option[Seq[String]],
                                        isOwner: Option[Boolean])

// CaseFileItem
case class CaseFileItem(id: String,
                        name: String,
                        caseInstanceId: String,
                        properties: Option[List[Property]],
                        parentCaseFileItemId: String,
                        content: String,
                        mimetype: String,
                        transition: String,
                        lastModified: Instant)


//base types
// Property of a caseFileItem
case class Property(name: String, `type`: String, value: String)
