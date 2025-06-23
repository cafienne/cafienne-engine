package org.cafienne.persistence.querydb.query.cmmn.authorization

import org.cafienne.actormodel.identity.UserIdentity
import org.cafienne.persistence.querydb.query.cmmn.implementations.BaseQueryImpl
import org.cafienne.persistence.querydb.query.exception.{CaseSearchFailure, TaskSearchFailure}
import org.cafienne.persistence.querydb.schema.QueryDB

import scala.concurrent.Future

class AuthorizationQueriesImpl(queryDB: QueryDB)
  extends BaseQueryImpl(queryDB) with AuthorizationQueries {

  import dbConfig.profile.api._

  override def getCaseMembership(caseInstanceId: String, user: UserIdentity): Future[CaseMembership] = {
    super.getCaseMembership(caseInstanceId, user, CaseSearchFailure, caseInstanceId)
  }

  override def getCaseMembershipForTask(taskId: String, user: UserIdentity): Future[CaseMembership] = {
    val result = for {
      caseId <- {
        db.run(TableQuery[TaskTable].filter(_.id === taskId).map(_.caseInstanceId).result).map(records =>
          if (records.isEmpty) throw TaskSearchFailure(taskId)
          else records.head
        )
      }
      membership <- getCaseMembership(caseId, user, TaskSearchFailure, taskId)
    } yield (caseId, membership)
    result.map(result => {
      //      println(s"Found membership for user ${user.id} on task $taskId in case ${result._1}")
      result._2
    })
  }

  override def getCaseOwnership(caseInstanceId: String, user: UserIdentity): Future[CaseOwnership] = {
    val groupMembership = TableQuery[CaseInstanceTeamGroupTable].filter(_.caseInstanceId === caseInstanceId)
      .join(TableQuery[ConsentGroupMemberTable].filter(_.userId === user.id))
      .on((caseGroup, group) => caseGroup.groupId === group.group && (caseGroup.groupRole === group.role || group.isOwner))
      .map(_._1)

    val tenantRoleBasedMembership = tenantRoleBasedMembershipQuery(caseInstanceId, user)
    val userIdBasedMembership = TableQuery[CaseInstanceTeamUserTable]
      .filter(_.caseInstanceId === caseInstanceId) // First filter on case id (probably faster than first on user id)
      .filter(_.userId === user.id)

    val query = for {
      groups <- db.run(groupMembership.result)
      roles <- db.run(tenantRoleBasedMembership.result)
      users <- db.run(userIdBasedMembership.result)
    } yield (groups, roles, users)

    query.map(result => {
      val groupMemberships = result._1
      val roleMemberships = result._2
      val userMembership = result._3
      if (groupMemberships.isEmpty && roleMemberships.isEmpty && userMembership.isEmpty) {
        throw CaseSearchFailure(caseInstanceId)
      }

      val isOwner = groupMemberships.exists(_.isOwner) || roleMemberships.exists(_.isOwner) || userMembership.exists(_.isOwner)
      val tenant = (groupMemberships.map(_.tenant) ++ roleMemberships.map(_.tenant) ++ userMembership.map(_.tenant)).toSet.head
      CaseOwnership(user.id, caseInstanceId, tenant, isOwner)
    })
  }
}
