package org.cafienne.service.api.projection.table

import java.time.Instant

import org.cafienne.service.api.projection.record._
import org.cafienne.service.db.querydb.QueryDBSchema
import slick.lifted.ColumnOrdered

trait CaseTables extends QueryDBSchema {

  import dbConfig.profile.api._

  //TODO: add lowercase index on definition in Postgresql to allow case insensitive searching

  class CaseInstanceTable(tag: Tag) extends CafienneTable[CaseRecord](tag, "case_instance") {
    override def getSortColumn(field: String): ColumnOrdered[_] = field match {
      case "id" => id
      case "definition" => caseName // Backwards compatibility; column name before was "definition"
      case "casename" => caseName
      case "name" => caseName
      case "status" => state
      case "state" => state
      case "tenant" => tenant
      case "failures" => failures
      case "parentcaseid" => parentCaseId
      case "rootcaseid" => rootCaseId
      case "modifiedny" => modifiedBy
      case "createdon" => createdOn
      case "createdby" => createdBy
      case "lastmodified" =>  lastModified
      case _ => lastModified
    }

    def id = idColumn[String]("id", O.PrimaryKey)

    def tenant = idColumn[String]("tenant")

    def caseName = idColumn[String]("case_name")

    def state = stateColumn[String]("state")

    def failures = column[Int]("failures")

    def parentCaseId = idColumn[String]("parent_case_id")

    def rootCaseId = idColumn[String]("root_case_id")

    def lastModified = column[Instant]("last_modified")

    def modifiedBy = userColumn[String]("modified_by")

    def createdOn = column[Instant]("created_on")

    def createdBy = userColumn[String]("created_by")

    def caseInput = jsonColumn[String]("case_input")

    def caseOutput = jsonColumn[String]("case_output")

    // Some indexes to optimize GetCases queries
    def indexState = index(state)
    def indexTenant = index(tenant)
    def indexRootCaseId = index(rootCaseId)
    def indexCaseName = index(caseName)
    def indexCreatedBy = index(createdBy)
    def indexModifiedBy = index(modifiedBy)

    def * = (id, tenant, caseName, state, failures, parentCaseId, rootCaseId, lastModified, modifiedBy, createdOn, createdBy, caseInput, caseOutput) <> (CaseRecord.tupled, CaseRecord.unapply)
  }

  final class CaseInstanceDefinitionTable(tag: Tag) extends CafienneTable[CaseDefinitionRecord](tag, "case_instance_definition") {

    def caseInstanceId = idColumn[String]("caseInstanceId", O.PrimaryKey)

    def name = idColumn[String]("name")

    def description = column[String]("description")

    def elementId = idColumn[String]("element_id")

    def content = column[String]("content")

    def tenant = idColumn[String]("tenant")

    def lastModified = column[Instant]("last_modified")

    def modifiedBy = userColumn[String]("modified_by")

    def * = (caseInstanceId, name, description, elementId, content, tenant, lastModified, modifiedBy) <> (CaseDefinitionRecord.tupled, CaseDefinitionRecord.unapply)
  }

  class PlanItemTable(tag: Tag) extends CafienneTable[PlanItemRecord](tag, "plan_item") {

    def id = idColumn[String]("id", O.PrimaryKey)

    def definitionId = idColumn[String]("definition_id", O.Default(""))

    def stageId = idColumn[String]("stage_id")

    def name = column[String]("name")

    def index = column[Int]("index")

    def caseInstanceId = idColumn[String]("case_instance_id")

    def tenant = idColumn[String]("tenant")

    def currentState = stateColumn[String]("current_state")

    def historyState = stateColumn[String]("history_state")

    def transition = stateColumn[String]("transition")

    def planItemType = stateColumn[String]("plan_item_type")

    def repeating = column[Boolean]("repeating")

    def required = column[Boolean]("required")

    def lastModified = column[Instant]("last_modified")

    def modifiedBy = userColumn[String]("modified_by")

    def createdOn = column[Instant]("created_on")

    def createdBy = userColumn[String]("created_by")

    def taskInput = jsonColumn[String]("task_input")

    def taskOutput = jsonColumn[String]("task_output")

    def mappedInput = jsonColumn[String]("mapped_input")

    def rawOutput = jsonColumn[String]("raw_output")

    def * = (id, definitionId, stageId, name, index, caseInstanceId, tenant, currentState, historyState, transition, planItemType, repeating, required, lastModified, modifiedBy, createdOn, createdBy, taskInput, taskOutput, mappedInput, rawOutput) <> (PlanItemRecord.tupled, PlanItemRecord.unapply)

    def indexCaseInstanceId = index(caseInstanceId)
    def indexCreatedBy = index(createdBy)
    def indexModifiedBy = index(modifiedBy)
  }

  final class PlanItemHistoryTable(tag: Tag) extends CafienneTable[PlanItemHistoryRecord](tag, "plan_item_history") {

    def id = idColumn[String]("id", O.PrimaryKey)

    def planItemId = idColumn[String]("plan_item_id")

    def stageId = idColumn[String]("stage_id")

    def name = column[String]("name")

    def index = column[Int]("index")

    def caseInstanceId = idColumn[String]("case_instance_id")

    def tenant = idColumn[String]("tenant")

    def currentState = stateColumn[String]("current_state")

    def historyState = stateColumn[String]("history_state")

    def transition = stateColumn[String]("transition")

    def planItemType = stateColumn[String]("plan_item_type")

    def repeating = column[Boolean]("repeating")

    def required = column[Boolean]("required")

    def lastModified = column[Instant]("last_modified")

    def modifiedBy = userColumn[String]("modified_by")

    def eventType = column[String]("eventType")

    def sequenceNr = column[Long]("sequenceNr")

    def taskInput = jsonColumn[String]("task_input")

    def taskOutput = jsonColumn[String]("task_output")

    def mappedInput = jsonColumn[String]("mapped_input")

    def rawOutput = jsonColumn[String]("raw_output")

    def * = (id, planItemId, stageId, name, index, caseInstanceId, tenant, currentState, historyState,transition, planItemType, repeating, required, lastModified, modifiedBy, eventType, sequenceNr, taskInput, taskOutput, mappedInput, rawOutput) <> (PlanItemHistoryRecord.tupled, PlanItemHistoryRecord.unapply)

    def idx = index("idx_plan_item_history__plain_item_id", planItemId)
    def indexModifiedBy = index(modifiedBy)
  }

  class CaseFileTable(tag: Tag) extends CafienneTable[CaseFileRecord](tag, "case_file") {

    def caseInstanceId = idColumn[String]("case_instance_id", O.PrimaryKey)

    def tenant = idColumn[String]("tenant")

    def data = jsonColumn[String]("data")

    def * = (caseInstanceId, tenant, data) <> (CaseFileRecord.tupled, CaseFileRecord.unapply)

    val indexCaseInstanceId = index(caseInstanceId)
  }

  final class CaseBusinessIdentifierTable(tag: Tag) extends CafienneTable[CaseBusinessIdentifierRecord](tag, "case_business_identifier") {

    def caseInstanceId = idColumn[String]("case_instance_id")

    def tenant = idColumn[String]("tenant")

    def name = idColumn[String]("name")

    def value = column[Option[String]]("value")

    def active = column[Boolean]("active")

    def path = column[String]("path")

    def * = (caseInstanceId, tenant, name, value, active, path) <> (CaseBusinessIdentifierRecord.tupled, CaseBusinessIdentifierRecord.unapply)

    val caseInstanceTable = TableQuery[CaseInstanceTable]

    def pk = primaryKey("pk_case_business_identifier", (caseInstanceId, name))

    val indexCaseInstanceId = index(caseInstanceId)
    val indexName = index(name)
  }

  class CaseInstanceRoleTable(tag: Tag) extends CafienneTable[CaseRoleRecord](tag, "case_instance_role") {

    def caseInstanceId = idColumn[String]("case_instance_id")

    def tenant = idColumn[String]("tenant")

    def roleName = idColumn[String]("role_name")

    def assigned = column[Boolean]("assigned") // true if team members are assigned for this role

    def pk = primaryKey("pk_case_instance_role", (caseInstanceId, roleName))

    def * = (caseInstanceId, tenant, roleName, assigned) <> (CaseRoleRecord.tupled, CaseRoleRecord.unapply)

    val indexCaseInstanceId = index(caseInstanceId)
  }

  class CaseInstanceTeamMemberTable(tag: Tag) extends CafienneTable[CaseTeamMemberRecord](tag, "case_instance_team_member") {

    def caseInstanceId = idColumn[String]("case_instance_id")

    def tenant = idColumn[String]("tenant")

    def caseRole = idColumn[String]("case_role")

    def memberId = userColumn[String]("member_id")

    def isTenantUser = column[Boolean]("isTenantUser")

    def isOwner = column[Boolean]("isOwner")

    def active = column[Boolean]("active")

    def pk = primaryKey("pk_case_instance_team_member", (caseInstanceId, caseRole, memberId, isTenantUser))

    def * = (caseInstanceId, tenant, memberId, caseRole, isTenantUser, isOwner, active) <> (CaseTeamMemberRecord.tupled, CaseTeamMemberRecord.unapply)

    val indexCaseInstanceId = index(caseInstanceId)
    def indexMemberId = index(generateIndexName(memberId), (memberId, isTenantUser))
  }
}
