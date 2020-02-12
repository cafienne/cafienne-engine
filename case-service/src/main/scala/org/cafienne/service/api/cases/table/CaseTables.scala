package org.cafienne.service.api.cases.table

import java.time.Instant

import org.cafienne.infrastructure.jdbc.QueryDbConfig
import org.cafienne.service.api.cases._
import slick.lifted

trait CaseTables extends QueryDbConfig {

  import dbConfig.profile.api._

  //TODO: add lowercase index on definition in Postgresql to allow case insensitive searching

  final class CaseInstanceTable(tag: Tag) extends CafienneTable[CaseInstance](tag, "case_instance") {

    def id = idColumn[String]("id", O.PrimaryKey)

    def tenant = idColumn[String]("tenant")

    def definition = idColumn[String]("definition")

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

    def * = (id, tenant, definition, state, failures, parentCaseId, rootCaseId, lastModified, modifiedBy, createdOn, createdBy, caseInput, caseOutput) <> (CaseInstance.tupled, CaseInstance.unapply)
  }

  final class CaseInstanceDefinitionTable(tag: Tag) extends CafienneTable[CaseInstanceDefinition](tag, "case_instance_definition") {

    def caseInstanceId = idColumn[String]("caseInstanceId", O.PrimaryKey)

    def name = idColumn[String]("name")

    def description = column[String]("description")

    def elementId = idColumn[String]("element_id")

    def content = column[String]("content")

    def tenant = idColumn[String]("tenant")

    def lastModified = column[Instant]("last_modified")

    def modifiedBy = idColumn[String]("modified_by")

    def * = (caseInstanceId, name, description, elementId, content, tenant, lastModified, modifiedBy) <> (CaseInstanceDefinition.tupled, CaseInstanceDefinition.unapply)
  }

  final class PlanItemTable(tag: Tag) extends CafienneTable[PlanItem](tag, "plan_item") {

    def id = idColumn[String]("id", O.PrimaryKey)

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

    def * = (id, stageId, name, index, caseInstanceId, tenant, currentState, historyState, transition, planItemType, repeating, required, lastModified, modifiedBy, createdOn, createdBy, taskInput, taskOutput, mappedInput, rawOutput) <> (PlanItem.tupled, PlanItem.unapply)

    val caseInstanceTable = lifted.TableQuery[CaseInstanceTable]

    def caseInstance = foreignKey("fk_plan_item__case_instance", caseInstanceId, caseInstanceTable)(_.id)

  }

  final class PlanItemHistoryTable(tag: Tag) extends CafienneTable[PlanItemHistory](tag, "plan_item_history") {

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

    def * = (id, planItemId, stageId, name, index, caseInstanceId, tenant, currentState, historyState,transition, planItemType, repeating, required, lastModified, modifiedBy, eventType, sequenceNr, taskInput, taskOutput, mappedInput, rawOutput) <> (PlanItemHistory.tupled, PlanItemHistory.unapply)

    val planItemHistoryTable = lifted.TableQuery[PlanItemHistoryTable]

    def idx = index("idx_plan_item_history__plain_item_id", (planItemId), unique = false)

    //    def caseInstance = foreignKey("fk_plan_item__case_instance", caseInstanceId, caseInstanceTable)(_.id)

  }

  final class CaseFileTable(tag: Tag) extends CafienneTable[CaseFile](tag, "case_file") {

    def caseInstanceId = idColumn[String]("case_instance_id", O.PrimaryKey)

    def tenant = idColumn[String]("tenant")

    def data = jsonColumn[String]("data")

    def * = (caseInstanceId, tenant, data) <> (CaseFile.tupled, CaseFile.unapply)

    val caseInstanceTable = lifted.TableQuery[CaseInstanceTable]

    def caseInstance = foreignKey("fk_case_file__case_instance", caseInstanceId, caseInstanceTable)(_.id)
  }

  final class CaseInstanceRoleTable(tag: Tag) extends CafienneTable[CaseInstanceRole](tag, "case_instance_role") {

    def caseInstanceId = idColumn[String]("case_instance_id")

    def tenant = idColumn[String]("tenant")

    def roleName = idColumn[String]("role_name")

    def assigned = column[Boolean]("assigned") // true if team members are assigned for this role

    def pk = primaryKey("pk_case_instance_role", (caseInstanceId, roleName))

    def * = (caseInstanceId, tenant, roleName, assigned) <> (CaseInstanceRole.tupled, CaseInstanceRole.unapply)

    val caseInstanceTable = lifted.TableQuery[CaseInstanceTable]

    def caseInstance = foreignKey("fk_case_instance_role__case_instance", caseInstanceId, caseInstanceTable)(_.id)
  }

  final class CaseInstanceTeamMemberTable(tag: Tag) extends CafienneTable[CaseInstanceTeamMember](tag, "case_instance_team_member") {

    def caseInstanceId = idColumn[String]("case_instance_id")

    def tenant = idColumn[String]("tenant")

    def role = idColumn[String]("role")

    def userId = idColumn[String]("user_id")

    def active = column[Boolean]("active")

    def pk = primaryKey("pk_case_instance_team_member", (caseInstanceId, role, userId))

    def * = (caseInstanceId, tenant, userId, role, active) <> (CaseInstanceTeamMember.tupled, CaseInstanceTeamMember.unapply)

    val caseInstanceTable = lifted.TableQuery[CaseInstanceTable]

    def caseInstance =
      foreignKey("fk_case_instance_team_member__case_instance", caseInstanceId, caseInstanceTable)(_.id)
  }

}
