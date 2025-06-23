package org.cafienne.persistence.querydb.query.cmmn.implementations.basequeries

import org.cafienne.actormodel.identity.UserIdentity
import org.cafienne.persistence.querydb.query.QueryDBReader

trait TenantRegistrationQueries extends QueryDBReader {
  import dbConfig.profile.api._

  /**
   * Returns a list of all tenants the user belongs to, and within each tenant all roles the user has in that tenant
   */
  def queryAllRolesInAllTenantsForUser(user: UserIdentity): Query[(Rep[String], Rep[String]), (String, String), Seq] = {
    val groupMemberships = TableQuery[ConsentGroupMemberTable].filter(_.userId === user.id)
      .join(TableQuery[ConsentGroupTable])
      .on(_.group === _.id)
      .map(record => (record._1.role, record._2.tenant))
    val tenantMemberships = TableQuery[UserRoleTable].filter(_.userId === user.id).map(record => (record.role_name, record.tenant))

    tenantMemberships.union(groupMemberships)
  }
}
