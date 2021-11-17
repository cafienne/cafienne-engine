package org.cafienne.service.api.model

import io.swagger.v3.oas.annotations.media.{ArraySchema, Schema}

import scala.annotation.meta.field

object Examples {
  @Schema(description = "Input parameters example json")
  case class InputParametersFormat(input1: String, input2: Object)

  @Schema(description = "Output parameters example json")
  case class OutputParametersFormat(output1: String, output2: Object, output3: List[String])

  @Schema(description = "Example case team member")
  case class CaseTeamUserResponseFormat(
                                           @(Schema@field)(
                                             description = "User id of the team member",
                                             example = "User id of the team member",
                                             implementation = classOf[String])
                                           userId: String,
                                           @(Schema@field)(
                                             description = "True if the user is a case owner. Case owners are authorized to manage the case and the case team",
                                             example = "True if the member is a case owner. Case owners are authorized to manage the case and the case team",
                                             implementation = classOf[CaseTeamUserIsOwnerFormat])
                                           isOwner: Boolean,
                                           @(ArraySchema@field)(schema = new Schema(
                                             description = "Zero or more case roles assigned to the user in this case. An empty set means that the member has no roles, but is still part of the team",
                                             example = "Zero or more case roles assigned to the user in this case. An empty set means that the member has no roles, but is still part of the team",
                                             implementation = classOf[CaseTeamUserCaseRolesFormat]))
                                           caseRoles: Array[String],
                                         )

  @Schema(description = "Example case team member")
  case class CaseTeamTenantRoleResponseFormat(
                                         @(Schema@field)(
                                           description = "All users in the tenant with this role are part of the case team",
                                           example = "All users in the tenant with this role are part of the case team",
                                           implementation = classOf[String])
                                         tenantRoleName: String,
                                         @(Schema@field)(
                                           description = "True if tenant users with this role are case owner. Case owners are authorized to manage the case and the case team",
                                           example = "True if tenant users with this role are case owner. Case owners are authorized to manage the case and the case team",
                                           implementation = classOf[CaseTeamTenantRoleIsOwnerFormat])
                                         isOwner: Boolean,
                                         @(ArraySchema@field)(schema = new Schema(
                                           description = "Zero or more case roles assigned to users with the tenant role in this case",
                                           example = "Zero or more case roles assigned to users with the tenant role in this case",
                                           implementation = classOf[CaseTeamTenantRoleCaseRolesFormat]))
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
                                       description = "Case roles that are not assigned to any of the team members",
                                       example = "Case roles that are not assigned to any of the team members",
                                       implementation = classOf[UnassignedRolesFormat]))
                                     unassignedRoles: Array[String],
                                     @(ArraySchema@field)(schema = new Schema(
                                       description = "Users in the case team",
                                       required = true,
                                       implementation = classOf[CaseTeamUserResponseFormat]))
                                     users: Array[CaseTeamFormat.CaseTeamUserFormat],
                                     @(ArraySchema@field)(schema = new Schema(
                                       description = "List of tenant roles that have access to the case",
                                       required = true,
                                       implementation = classOf[CaseTeamTenantRoleResponseFormat]))
                                     tenantRoles: Array[CaseTeamFormat.TenantRoleFormat])

  case class CaseDefinedRolesFormat()

  case class UnassignedRolesFormat()

  case class CaseTeamUserIsOwnerFormat()

  case class CaseTeamUserCaseRolesFormat()

  case class CaseTeamTenantRoleCaseRolesFormat()

  case class CaseTeamTenantRoleIsOwnerFormat()
}
