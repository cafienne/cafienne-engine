package org.cafienne.service.api.cases

import java.time.Instant

import org.cafienne.cmmn.instance.casefile.{JSONReader, Value, ValueMap}
import org.cafienne.infrastructure.jdbc.QueryDbConfig
import slick.lifted

final case class CaseInstance(id: String,
                              tenant: String,
                              name: String,
                              state: String,
                              failures: Int,
                              parentCaseId: String = "",
                              rootCaseId: String,
                              lastModified: Instant,
                              modifiedBy: String = "",
                              createdOn: Instant,
                              createdBy: String = "",
                              caseInput: String = "",
                              caseOutput: String = "") {

  def toValueMap: ValueMap = {
    val v = new ValueMap
    v.putRaw("id", id)
    v.putRaw("tenant", tenant)
    v.putRaw("definition", name)
    v.putRaw("name", name)
    v.putRaw("state", state)
    v.putRaw("failures", failures)
    v.putRaw("parentCaseId", parentCaseId)
    v.putRaw("rootCaseId", rootCaseId)
    v.putRaw("createdOn", createdOn)
    v.putRaw("createdBy", createdBy)
    v.putRaw("lastModified", lastModified)
    v.putRaw("modifiedBy", modifiedBy)
    v.putRaw("caseInput", modifiedBy)
    v.putRaw("caseOutput", modifiedBy)
    v.putRaw("lastModifiedBy", modifiedBy) // Temporary compatibility
    v
  }

}

final case class CaseInstanceDefinition(caseInstanceId: String, name: String, description: String, elementId: String, content: String, tenant: String, lastModified: Instant, modifiedBy: String)

final case class CaseFile(caseInstanceId: String, tenant: String, data: String) {
  def toValueMap: ValueMap = JSONReader.parse(data)
}

final case class CaseInstanceRole(caseInstanceId: String, tenant: String, roleName: String, assigned: Boolean = true)

final case class CaseInstanceTeamMember(caseInstanceId: String, tenant: String, userId: String, role: String, active: Boolean)

final case class PlanItem(id: String,
                          stageId: String,
                          name: String,
                          index: Int,
                          caseInstanceId: String,
                          tenant: String,
                          currentState: String = "",
                          historyState: String = "",
                          transition: String = "",
                          planItemType: String,
                          repeating: Boolean = false,
                          required: Boolean = false,
                          lastModified: Instant,
                          modifiedBy: String,
                          createdOn: Instant,
                          createdBy: String = "",
                          taskInput: String = "",
                          taskOutput: String = "",
                          mappedInput: String = "",
                          rawOutput: String = "") {

  def toValueMap: ValueMap = {
    val v = new ValueMap
    v.putRaw("isRequired", required)
    v.putRaw("isRepeating", repeating)
    v.putRaw("caseInstanceId", caseInstanceId)
    v.putRaw("id", id)
    v.putRaw("name", name)
    v.putRaw("index", index)
    v.putRaw("stageId", stageId)
    v.putRaw("currentState", currentState)
    v.putRaw("historyState", historyState)
    v.putRaw("type", planItemType)
    v.putRaw("transition", transition)
    v.putRaw("lastModified", lastModified)
    v.putRaw("modifiedBy", modifiedBy)
    v.putRaw("user", modifiedBy) // Temporary compatibility
    v
  }

}

final case class PlanItemHistory(id: String,
                                 planItemId: String,
                                 stageId: String = "",
                                 name: String = "",
                                 index: Int,
                                 caseInstanceId: String,
                                 tenant: String,
                                 currentState: String = "",
                                 historyState: String = "",
                                 transition: String = "",
                                 planItemType: String = "",
                                 repeating: Boolean = false,
                                 required: Boolean = false,
                                 lastModified: Instant,
                                 modifiedBy: String,
                                 eventType: String,
                                 sequenceNr: Long,
                                 taskInput: String = "",
                                 taskOutput: String = "",
                                 mappedInput: String = "",
                                 rawOutput: String = "") {



  def toValueMap: ValueMap = {
    val v = new ValueMap
    v.putRaw("id", id)
    v.putRaw("planItemId", id)
    v.putRaw("name", name)
    v.putRaw("index", index)
    v.putRaw("stageId", stageId)
    v.putRaw("caseInstanceId", caseInstanceId)
    v.putRaw("currentState", currentState)
    v.putRaw("historyState", historyState)
    v.putRaw("type", planItemType)
    v.putRaw("transition", transition)
    v.putRaw("repeating", repeating)
    v.putRaw("required", required)
    v.putRaw("lastModified", lastModified)
    v.putRaw("modifiedBy", modifiedBy)
    v.putRaw("eventType", eventType)
    v.putRaw("sequenceNr", sequenceNr)
    v
  }

}


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
