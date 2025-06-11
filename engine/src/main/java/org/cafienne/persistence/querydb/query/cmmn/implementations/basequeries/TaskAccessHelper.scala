package org.cafienne.persistence.querydb.query.cmmn.implementations.basequeries

import org.cafienne.actormodel.identity.UserIdentity

trait TaskAccessHelper extends TenantRegistrationQueries {
  import dbConfig.profile.api._

  /**
   * Returns the case roles and whether the user has ownership of the case
   */
  def queryCaseAccess(user: UserIdentity, caseInstanceId: Rep[String]): Query[(Rep[String], Rep[String], Rep[Boolean]), (String, String, Boolean), Seq] = {
    consentGroupCoupledCaseRoles(user).unionAll(tenantRoleCoupledCaseRoles(user)).unionAll(userCoupledCaseRoles(user)).filter(_._1 === caseInstanceId)
  }

  // First define 3 base queries that help find the cases that the user has access to with the specific case roles.
  //  Resulting queries give a list of case instance id / case role pairs.
  def consentGroupCoupledCaseRoles(user: UserIdentity): Query[(Rep[String], Rep[String], Rep[Boolean]), (String, String, Boolean), Seq] = {
    TableQuery[ConsentGroupMemberTable].filter(_.userId === user.id)
      .join(TableQuery[CaseInstanceTeamGroupTable]).on((group, membership) => group.role === membership.groupRole && group.group === membership.groupId).map(_._2)
      .map(group => (group.caseInstanceId, group.caseRole, group.isOwner))
  }

  def tenantRoleCoupledCaseRoles(user: UserIdentity): Query[(Rep[String], Rep[String], Rep[Boolean]), (String, String, Boolean), Seq] = {
    queryAllRolesInAllTenantsForUser(user)
      .join(TableQuery[CaseInstanceTeamTenantRoleTable])
      .on((tenantRoles, caseMembership) =>
        // Either
        (tenantRoles._1 === caseMembership.tenantRole)
          && tenantRoles._2 === caseMembership.tenant).map(_._2)
      .map(tenantRole => (tenantRole.caseInstanceId, tenantRole.caseRole, tenantRole.isOwner))
  }

  def userCoupledCaseRoles(user: UserIdentity): Query[(Rep[String], Rep[String], Rep[Boolean]), (String, String, Boolean), Seq] = {
    TableQuery[CaseInstanceTeamUserTable]
      .filter(_.userId === user.id)
      .map(user => (user.caseInstanceId, user.caseRole, user.isOwner))
  }
}
