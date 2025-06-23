package org.cafienne.persistence.querydb.query.cmmn.authorization

import org.cafienne.actormodel.identity.UserIdentity

import scala.concurrent.Future

trait AuthorizationQueries {
  def getCaseMembership(caseInstanceId: String, user: UserIdentity): Future[CaseMembership]
  def getCaseOwnership(caseInstanceId: String, user: UserIdentity): Future[CaseOwnership]
  def getCaseMembershipForTask(taskId: String, user: UserIdentity): Future[CaseMembership]
}
