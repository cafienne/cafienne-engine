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

package org.cafienne.service.akkahttp.cases.team

import io.swagger.v3.oas.annotations.media.{ArraySchema, Schema}
import org.cafienne.actormodel.identity.Origin
import org.cafienne.cmmn.actorapi.command.team._
import org.cafienne.infrastructure.akkahttp.EntityReader.{EntityReader, entityReader}
import org.cafienne.service.akkahttp.ApiValidator

import scala.annotation.meta.field

object CaseTeamAPI {
  implicit val teamReader: EntityReader[TeamFormat] = entityReader[TeamFormat]
  implicit val userMemberReader: EntityReader[CaseTeamUserFormat] = entityReader[CaseTeamUserFormat]
  implicit val groupMemberReader: EntityReader[GroupFormat] = entityReader[GroupFormat]
  implicit val roleMemberReader: EntityReader[TenantRoleFormat] = entityReader[TenantRoleFormat]

  case class TeamFormat(users: Seq[CaseTeamUserFormat], groups: Seq[GroupFormat], tenantRoles: Seq[TenantRoleFormat]) {
    ApiValidator.runDuplicatesDetector("Case team", "user", users.map(_.userId))
    ApiValidator.runDuplicatesDetector("Case team", "group", groups.map(_.groupId))
    ApiValidator.runDuplicatesDetector("Case team", "tenant role", tenantRoles.map(_.tenantRole))

    def asTeam: CaseTeam = CaseTeam(users = users.map(_.asCaseTeamUser), groups = groups.map(_.asGroup), tenantRoles = tenantRoles.map(_.asTenantRole))
  }

  case class CaseTeamUserFormat(
                                 @(Schema@field)(
                                   description = "Unique identifier of the user",
                                   example = "Unique identifier of the user",
                                   required = true,
                                   implementation = classOf[String])
                                 userId: String = "",
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
                                 caseRoles: Seq[String] = Seq()) {
    ApiValidator.required(userId, "Case team users must have a userId")

    def asCaseTeamUser: CaseTeamUser = CaseTeamUser.from(userId = userId, origin = Origin.IDP, caseRoles = caseRoles.toSet, isOwner = isOwner.getOrElse(false))
  }

  case class TenantRoleFormat(
                               @(Schema@field)(
                                 description = "All users in the tenant with this role are member of the case team",
                                 example = "All users in the tenant with this role are member of the case team",
                                 required = true,
                                 implementation = classOf[String])
                               tenantRole: String = "",
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
                               caseRoles: Seq[String] = Seq()) {
    ApiValidator.required(tenantRole, "Tenant role members must have a valid role name")

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
                          groupId: String = "",
                          @(ArraySchema@field)(schema = new Schema(
                            description = "Mappings of consent group role to case team roles",
                            required = true,
                            implementation = classOf[RoleMappingFormat]))
                          mappings: Seq[RoleMappingFormat] = Seq(),
                        ) {
    // Group id must be filled, validation on existence is done further down the line.
    ApiValidator.required(groupId, "Case team groups must have a groupId")
    // Mappings must be set
    if (mappings.isEmpty) {
      throw new IllegalArgumentException("Case team groups must have one or more mappings defined")
    }

    def asGroup: CaseTeamGroup = {
      CaseTeamGroup(groupId = groupId, mappings = mappings.map(_.asGroupRoleMapping))
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
                                caseRoles: Seq[String] = Seq(),
                              ) {
    def asGroupRoleMapping: GroupRoleMapping = GroupRoleMapping(groupRole = groupRole, isOwner = isOwner.getOrElse(false), caseRoles = caseRoles.toSet)
  }

  // Simple classes to enable different example/description for each of the usages of 'caseRoles'
  case class NewCaseTeamRolesForUser()

  case class NewCaseTeamRolesForTenantRole()

  case class RemoveCaseTeamRolesFromUser()

  case class RemoveCaseTeamRolesFromTenantRole()

  /**
    *
    *
    */
  object Compatible {
    implicit val teamReader: EntityReader[TeamFormat] = entityReader[TeamFormat]
    implicit val userReader: EntityReader[BackwardCompatibleTeamMemberFormat] = entityReader[BackwardCompatibleTeamMemberFormat]

    case class TeamFormat(
                           users: Seq[CaseTeamAPI.CaseTeamUserFormat] = Seq(),
                           groups: Seq[CaseTeamAPI.GroupFormat] = Seq(),
                           tenantRoles: Seq[CaseTeamAPI.TenantRoleFormat] = Seq(),
                           members: Seq[BackwardCompatibleTeamMemberFormat] = Seq()) {
      private lazy val getUsers: Seq[CaseTeamUser] = users.map(_.asCaseTeamUser) ++ members.filter(_.isUser).map(_.asUser)
      private lazy val getGroups: Seq[CaseTeamGroup] = groups.map(_.asGroup)
      private lazy val getRoles: Seq[CaseTeamTenantRole] = tenantRoles.map(_.asTenantRole) ++ members.filterNot(_.isUser).map(_.asTenantRole)

      // Run validations: users, groups and roles should not contain duplicates
      //  Note, in addition individual members do their own validations
      ApiValidator.runDuplicatesDetector("Case team", "user", users.map(_.userId))
      ApiValidator.runDuplicatesDetector("Case team", "group", groups.map(_.groupId))
      ApiValidator.runDuplicatesDetector("Case team", "tenant role", tenantRoles.map(_.tenantRole))

      def asTeam: CaseTeam = CaseTeam(users = getUsers, groups = getGroups, tenantRoles = getRoles)
    }


    case class BackwardCompatibleTeamMemberFormat(user: Option[String], // Old property, to be ccompatiblty
                                                  roles: Option[Set[String]], // Old property, just keep it here to remain compatible
                                                  // New structure below
                                                  memberId: Option[String],
                                                  memberType: Option[String],
                                                  removeRoles: Option[Set[String]],
                                                  caseRoles: Option[Set[String]],
                                                  isOwner: Option[Boolean]) {
      lazy val isUser: Boolean = memberType.getOrElse("user") != "role"
      private lazy val getRoles = caseRoles.getOrElse(roles.getOrElse(Set()))
      private lazy val identifier = memberId.getOrElse(user.getOrElse(throw new IllegalArgumentException("Member id is missing; consider migrating to the new case team format")))
      private lazy val ownership = {
        if (isOwner.nonEmpty) isOwner.get // If the value of owner is filled, then that precedes (both in old and new format)
        else if (user.nonEmpty) true // Old format ==> all users become owner
        else false // New format, take what is set
      }

      def upsertMemberData: UpsertMemberData = {
        new UpsertMemberData(id = identifier, isUser = isUser, caseRoles = getRoles, removeRoles = removeRoles.getOrElse(Set()), ownership = isOwner)
      }

      def asTenantRole = new CaseTeamTenantRole(tenantRoleName = identifier, caseRoles = getRoles, isOwner = ownership)

      // Note: this no longer takes removeRoles into account, as the new user will replace existing user
      //  if this leads to issues with someone, then we can provide a fix
      def asUser: CaseTeamUser = CaseTeamUser.from(userId = identifier, origin = Origin.IDP, caseRoles = getRoles, isOwner = ownership)
    }
  }

  object Examples {
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
                                           caseRoles: Seq[String],
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
                                                 caseRoles: Seq[String],
                                               )

    @Schema(description = "Example case team")
    case class CaseTeamResponseFormat(
                                       @(ArraySchema@field)(schema = new Schema(
                                         description = "Names of roles as defined in the case definition",
                                         example = "Names of roles as defined in the case definition",
                                         implementation = classOf[CaseDefinedRolesFormat]))
                                       caseRoles: Seq[String],
                                       @(ArraySchema@field)(schema = new Schema(
                                         description = "Case roles that are not assigned to any of the team members",
                                         example = "Case roles that are not assigned to any of the team members",
                                         implementation = classOf[UnassignedRolesFormat]))
                                       unassignedRoles: Seq[String],
                                       @(ArraySchema@field)(schema = new Schema(
                                         description = "Users in the case team",
                                         required = true,
                                         implementation = classOf[CaseTeamUserResponseFormat]))
                                       users: Seq[CaseTeamAPI.CaseTeamUserFormat],
                                       @(ArraySchema@field)(schema = new Schema(
                                         description = "Consent groups in the case team",
                                         required = true,
                                         implementation = classOf[CaseTeamAPI.GroupFormat]))
                                       groups: Seq[CaseTeamAPI.GroupFormat],
                                       @(ArraySchema@field)(schema = new Schema(
                                         description = "List of tenant roles that have access to the case",
                                         required = true,
                                         implementation = classOf[CaseTeamTenantRoleResponseFormat]))
                                       tenantRoles: Seq[CaseTeamAPI.TenantRoleFormat])

    case class CaseDefinedRolesFormat()

    case class UnassignedRolesFormat()

    case class CaseTeamUserIsOwnerFormat()

    case class CaseTeamUserCaseRolesFormat()

    case class CaseTeamTenantRoleCaseRolesFormat()

    case class CaseTeamTenantRoleIsOwnerFormat()
  }
}
