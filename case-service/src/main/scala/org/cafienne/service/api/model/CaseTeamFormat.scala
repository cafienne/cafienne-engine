package org.cafienne.service.api.model

import io.swagger.v3.oas.annotations.media.{ArraySchema, Schema}
import org.cafienne.actormodel.identity.Origin
import org.cafienne.cmmn.actorapi.command.team.{CaseTeamGroup, CaseTeamTenantRole, CaseTeamUser, GroupRoleMapping}

import scala.annotation.meta.field

object CaseTeamFormat {

  case class TeamFormat(users: Array[CaseTeamUserFormat], groups: Array[GroupFormat], tenantRoles: Array[TenantRoleFormat])

  case class CaseTeamUserFormat(
                         @(Schema@field)(
                           description = "Unique identifier of the user",
                           example = "Unique identifier of the user",
                           required = true,
                           implementation = classOf[String])
                         userId: String,
                         @(Schema@field)(
                           required = false,
                           implementation = classOf[Option[Boolean]],
                           description = "Optional field to set case ownership; defaults to false",
                           example = "Optional field to set case ownership; defaults to false")
                         isOwner: Option[Boolean] = None,
                         @(ArraySchema@field)(schema = new Schema(
                           required = true,
                           implementation = classOf[NewCaseTeamRolesForUser],
                           description = "Zero or more case roles that will be added to the user",
                           example = "Zero or more case roles that will be added to the user"))
                         caseRoles: Array[String]) {
    def asCaseTeamUser: CaseTeamUser = CaseTeamUser.from(userId = userId, origin = Origin.IDP, caseRoles = caseRoles.toSet, isOwner = isOwner.getOrElse(false))
  }

  case class TenantRoleFormat(
                               @(Schema@field)(
                                 description = "All users in the tenant with this role are member of the case team",
                                 example = "All users in the tenant with this role are member of the case team",
                                 required = true,
                                 implementation = classOf[String])
                               tenantRole: String,
                               @(Schema@field)(
                                 required = false,
                                 implementation = classOf[Option[Boolean]],
                                 description = "Optional field to set case ownership; defaults to false",
                                 example = "Optional field to set case ownership; defaults to false")
                               isOwner: Option[Boolean] = None,
                               @(ArraySchema@field)(schema = new Schema(
                                 required = true,
                                 implementation = classOf[NewCaseTeamRolesForTenantRole],
                                 example = "Zero or more case roles that will be added to the tenant role based membership"))
                               caseRoles: Array[String]){
    def asTenantRole: CaseTeamTenantRole = {
      CaseTeamTenantRole(tenantRoleName = tenantRole, caseRoles = caseRoles.toSet, isOwner = isOwner.getOrElse(false))
    }
  }

  case class GroupFormat(
                          @(Schema@field)(
                            description = "Identification of the consent group",
                            example = "Identification of the consent group",
                            required = true,
                            implementation = classOf[String])
                          groupId: String,
                          @(ArraySchema@field)(schema = new Schema(
                            description = "Mappings of consent group role to case team role",
                            required = true,
                            implementation = classOf[RoleMappingFormat]))
                          mappings: Array[RoleMappingFormat],
                        ) {
    def asGroup: CaseTeamGroup = {
      CaseTeamGroup(groupId = groupId, mappings = mappings.toSeq.map(_.asGroupRoleMapping))
    }
  }

  case class RoleMappingFormat(
                                 @(Schema@field)(
                                   description = "Name of the group role that maps to the specified case role",
                                   example = "Name of the group role that maps to the specified case role",
                                   required = true,
                                   implementation = classOf[String])
                                 groupRole: String,
                                 @(Schema@field)(
                                   required = false,
                                   implementation = classOf[Option[Boolean]],
                                   description = "Optional field to fill when case ownership needs to be set on the group role",
                                   example = "Optional field to fill when case ownership needs to be set on the group role")
                                 isOwner: Option[Boolean] = None,
                                @(Schema@field)(
                                  description = "Users in the group with the specified group role have the case role inside the case team",
                                  example = "[A, B, C]",
                                  required = true,
                                  implementation = classOf[String])
                                caseRoles: Array[String],
                              ) {
    def asGroupRoleMapping: GroupRoleMapping = GroupRoleMapping(caseRoles = caseRoles.toSet, groupRole = groupRole, isOwner = isOwner.getOrElse(false))
  }

  // Simple classes to enable different example/description for each of the usages of 'caseRoles'
  case class NewCaseTeamRolesForUser()

  case class NewCaseTeamRolesForTenantRole()

  case class RemoveCaseTeamRolesFromUser()

  case class RemoveCaseTeamRolesFromTenantRole()
}