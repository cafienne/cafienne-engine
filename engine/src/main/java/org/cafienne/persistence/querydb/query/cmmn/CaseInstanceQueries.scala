package org.cafienne.persistence.querydb.query.cmmn

import org.cafienne.actormodel.identity.UserIdentity
import org.cafienne.persistence.querydb.query.result.{CaseFileDocumentation, CaseTeamResponse, Documentation, FullCase}
import org.cafienne.persistence.querydb.record.{CaseDefinitionRecord, CaseFileRecord, CaseRecord, PlanItemRecord, TaskRecord}

import scala.concurrent.Future

trait CaseInstanceQueries {
  def getFullCaseInstance(caseInstanceId: String, user: UserIdentity): Future[FullCase]

  def getCaseDefinition(caseInstanceId: String, user: UserIdentity): Future[CaseDefinitionRecord]

  def getCaseInstance(caseInstanceId: String, user: UserIdentity): Future[Option[CaseRecord]]

  def getCaseFile(caseInstanceId: String, user: UserIdentity): Future[CaseFileRecord]

  def getCaseFileDocumentation(caseInstanceId: String, user: UserIdentity): Future[CaseFileDocumentation]

  def getCaseTeam(caseInstanceId: String, user: UserIdentity): Future[CaseTeamResponse]

  def getPlanItems(caseInstanceId: String, user: UserIdentity): Future[Seq[PlanItemRecord]]

  def getPlanItem(planItemId: String, user: UserIdentity): Future[PlanItemRecord]

  def getPlanItemDocumentation(planItemId: String, user: UserIdentity): Future[Documentation]

  def getCaseTasks(caseInstanceId: String, user: UserIdentity): Future[Seq[TaskRecord]]
}
