package org.cafienne.service.api.model

import io.swagger.v3.oas.annotations.media.{ArraySchema, Schema}

import scala.annotation.meta.field

final object Examples {
  @Schema(description = "Input parameters example json")
  case class InputParametersFormat(input1: String, input2: Object)

  @Schema(description = "Output parameters example json")
  case class OutputParametersFormat(output1: String, output2: Object, output3: List[String])

  @Schema(description = "Example case team")
  case class StartCaseTeamFormat(
                                  @(ArraySchema@field)(schema = new Schema(
                                    description = "If members is left empty, only current user will be added to the case team",
                                    required = true,
                                    implementation = classOf[StartCaseTeamMemberFormat]))
                                  members: Array[StartCaseTeamMemberFormat]
                                )

  @Schema(description = "Example case team member")
  case class StartCaseTeamMemberFormat(
                                        @(Schema@field)(
                                          description = "Identification of the team member (either user id or tenant role name)",
                                          example = "Identification of the team member (either user id or tenant role name)",
                                          required = true,
                                          implementation = classOf[String])
                                        memberId: String,
                                        @(Schema@field)(
                                          description = "Type of member, either 'user' or 'role'. If a member is of type 'role', then all tenant users with that role belong to the case team",
                                          required = false,
                                          implementation = classOf[String],
                                          example = "This field must contain either 'user' or 'role'. Defaults to 'user'. If 'role' is given, the memberId must contain a tenant role name, otherwise it must be a tenant user id",
                                          allowableValues = Array("user", "role"))
                                        memberType: String,
                                        @(Schema@field)(
                                          description = "Whether the member is owner to the case",
                                          required = false,
                                          implementation = classOf[Boolean],
                                          example = "false or true")
                                        isOwner: Boolean = false,
                                        @(ArraySchema@field)(schema = new Schema(
                                          description = "Zero or more roles that the member has in the case team. An empty set means that the member has no roles, but is still part of the team",
                                          required = true,
                                          implementation = classOf[NewCaseTeamRolesFormat],
                                          example = "[\"Employee\",  \"Customer\", \"Supplier\"]"))
                                        caseRoles: Array[String]
                                      )

  @Schema(description = "Example case team member")
  case class PutCaseTeamMemberFormat(
                                      @(Schema@field)(
                                        description = "Identification of the team member (either user id or tenant role name)",
                                        example = "Identification of the team member (either user id or tenant role name)",
                                        required = true,
                                        implementation = classOf[String])
                                      memberId: String,
                                      @(Schema@field)(
                                        description = "Type of member, either 'user' or 'role'. If a member is of type 'role', then all tenant users with that role belong to the case team",
                                        required = false,
                                        implementation = classOf[String],
                                        example = "This field must contain either 'user' or 'role'. Defaults to 'user'. If 'role' is given, the memberId must contain a tenant role name, otherwise it must be a tenant user id",
                                        allowableValues = Array("user", "role"))
                                      memberType: String,
                                      @(Schema@field)(
                                        description = "Optional field to fill when ownership needs to change for the member",
                                        required = false,
                                        implementation = classOf[Option[Boolean]],
                                        example = "Optional field to fill when ownership needs to change for the member; passing true will add ownership; passing false removes; not passing does not change existing ownership rights")
                                      isOwner: Option[Boolean] = None,
                                      @(ArraySchema@field)(schema = new Schema(
                                        description = "Zero or more case roles that will be added to the member",
                                        required = true,
                                        implementation = classOf[NewCaseTeamRolesFormat],
                                        example = "Zero or more case roles that will be added to the member"))
                                      caseRoles: Array[String],
                                      @(ArraySchema@field)(schema = new Schema(
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
                                           @(Schema@field)(
                                             description = "Identification of the team member (either user id or tenant role name)",
                                             example = "Identification of the team member (either user id or tenant role name)",
                                             implementation = classOf[String])
                                           memberId: String,
                                           @(Schema@field)(
                                             description = "Type of member, either 'user' or 'role'. If a member is of type 'role', then all tenant users with that role belong to the case team",
                                             implementation = classOf[String],
                                             example = "Type of member, either 'user' or 'role'. If a member is of type 'role', then all tenant users with that role belong to the case team",
                                             allowableValues = Array("user", "role"))
                                           memberType: String,
                                           @(Schema@field)(
                                             description = "True if the member is a Case Owner; Case Owners are authorized to manage the case and the case team",
                                             implementation = classOf[Option[Boolean]],
                                             example = "True if the member is a Case Owner; Case Owners are authorized to manage the case and the case team")
                                           isOwner: Boolean,
                                           @(ArraySchema@field)(schema = new Schema(
                                             description = "Zero or more case roles that will be added to the member",
                                             implementation = classOf[NewCaseTeamRolesFormat],
                                             example = "Zero or more case roles that will be added to the member"))
                                           caseRoles: Array[String],
                                         )

  @Schema(description = "Example case team")
  case class CaseTeamResponseFormat(
                                     @(ArraySchema@field)(schema = new Schema(
                                       description = "Names of roles as defined in the case definition",
                                       example = "Names of roles as defined in the case definition",
                                       implementation = classOf[CaseDefinedRolesFormat]))
                                     caseRoles: Array[String],
                                     @(ArraySchema@field)(schema = new Schema(
                                       description = "Members of the case team",
                                       required = true,
                                       implementation = classOf[CaseTeamMemberResponseFormat]))
                                     members: Array[CaseTeamMemberResponseFormat],
                                     @(ArraySchema@field)(schema = new Schema(
                                       description = "Names of defined roles that are not assigned to any of the team members",
                                       example = "Names of defined roles that are not assigned to any of the team members",
                                       implementation = classOf[UnassignedRolesFormat]))
                                     unassignedRoles: Array[String]
                                   )

  case class CaseDefinedRolesFormat()

  case class UnassignedRolesFormat()

  case class CaseTeamRolesFormat()
}
