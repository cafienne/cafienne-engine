package org.cafienne.service.api.cases

import java.time.Instant

import org.cafienne.cmmn.instance.casefile.{JSONReader, Value, ValueMap}
import org.cafienne.infrastructure.jdbc.DbConfig
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
                              createdBy: String = "") {

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
                          taskInput: Option[String] = None,
                          taskOutput: Option[String] = None,
                          mappedInput: Option[String] = None,
                          rawOutput: Option[String] = None) {

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
                                 lastModified: Option[Instant],
                                 modifiedBy: String,
                                 eventType: String,
                                 sequenceNr: Long) {

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
    v.putRaw("lastModified", lastModified.getOrElse(""))
    v.putRaw("modifiedBy", modifiedBy)
    v.putRaw("eventType", eventType)
    v.putRaw("sequenceNr", sequenceNr)
    v
  }

}


trait CaseTables extends DbConfig {

  import dbConfig.profile.api._

  //TODO: add lowercase index on definition in Postgresql to allow case insensitive searching

  final class CaseInstanceTable(tag: Tag) extends Table[CaseInstance](tag, "case_instance") {

    def id = column[String]("id", O.PrimaryKey)

    def tenant = column[String]("tenant")

    def definition = column[String]("definition")

    def state = column[String]("state")

    def failures = column[Int]("failures")

    def parentCaseId = column[String]("parent_case_id")

    def rootCaseId = column[String]("root_case_id")

    def lastModified = column[Instant]("last_modified")

    def modifiedBy = column[String]("modified_by")

    def createdOn = column[Instant]("created_on")

    def createdBy = column[String]("created_by")

    def * = (id, tenant, definition, state, failures, parentCaseId, rootCaseId, lastModified, modifiedBy, createdOn, createdBy) <> (CaseInstance.tupled, CaseInstance.unapply)
  }

  final class CaseInstanceDefinitionTable(tag: Tag) extends Table[CaseInstanceDefinition](tag, "case_instance_definition") {
    def caseInstanceId = column[String]("caseInstanceId", O.PrimaryKey)

    def name = column[String]("name")

    def description = column[String]("description")

    def elementId = column[String]("element_id")

    def content = column[String]("content")

    def tenant = column[String]("tenant")

    def lastModified = column[Instant]("last_modified")

    def modifiedBy = column[String]("modified_by")

    def * = (caseInstanceId, name, description, elementId, content, tenant, lastModified, modifiedBy) <> (CaseInstanceDefinition.tupled, CaseInstanceDefinition.unapply)
  }

  final class PlanItemTable(tag: Tag) extends Table[PlanItem](tag, "plan_item") {

    def id = column[String]("id", O.PrimaryKey)

    def stageId = column[String]("stage_id")

    def name = column[String]("name")

    def index = column[Int]("index")

    def caseInstanceId = column[String]("case_instance_id")

    def tenant = column[String]("tenant")

    def currentState = column[String]("current_state")

    def historyState = column[String]("history_state")

    def transition = column[String]("transition")

    def planItemType = column[String]("plan_item_type")

    def repeating = column[Boolean]("repeating")

    def required = column[Boolean]("required")

    def lastModified = column[Instant]("last_modified")

    def modifiedBy = column[String]("modified_by")

    def createdOn = column[Instant]("created_on")

    def createdBy = column[String]("created_by")

    def taskInput = column[Option[String]]("task_input")

    def taskOutput = column[Option[String]]("task_output")

    def mappedInput = column[Option[String]]("mapped_input")

    def rawOutput = column[Option[String]]("raw_output")

    def * = (id, stageId, name, index, caseInstanceId, tenant, currentState, historyState, transition, planItemType, repeating, required, lastModified, modifiedBy, createdOn, createdBy, taskInput, taskOutput, mappedInput, rawOutput) <> (PlanItem.tupled, PlanItem.unapply)

    val caseInstanceTable = lifted.TableQuery[CaseInstanceTable]

    def caseInstance = foreignKey("fk_plan_item__case_instance", caseInstanceId, caseInstanceTable)(_.id)

  }

  final class PlanItemHistoryTable(tag: Tag) extends Table[PlanItemHistory](tag, "plan_item_history") {

    def id = column[String]("id", O.PrimaryKey)

    def planItemId = column[String]("plan_item_id")

    def stageId = column[String]("stage_id")

    def name = column[String]("name")

    def index = column[Int]("index")

    def caseInstanceId = column[String]("case_instance_id")

    def tenant = column[String]("tenant")

    def currentState = column[String]("current_state")

    def historyState = column[String]("history_state")

    def transition = column[String]("transition")

    def planItemType = column[String]("plan_item_type")

    def repeating = column[Boolean]("repeating")

    def required = column[Boolean]("required")

    def lastModified = column[Option[Instant]]("last_modified")

    def modifiedBy = column[String]("modified_by")

    def eventType = column[String]("eventType")

    def sequenceNr = column[Long]("sequenceNr")

    def * = (id, planItemId, stageId, name, index, caseInstanceId, tenant, currentState, historyState,transition, planItemType, repeating, required, lastModified, modifiedBy, eventType,sequenceNr) <> (PlanItemHistory.tupled, PlanItemHistory.unapply)

    val planItemHistoryTable = lifted.TableQuery[PlanItemHistoryTable]

    def idx = index("idx_plan_item_history__plain_item_id", (planItemId), unique = false)

    //    def caseInstance = foreignKey("fk_plan_item__case_instance", caseInstanceId, caseInstanceTable)(_.id)

  }

  final class CaseFileTable(tag: Tag) extends Table[CaseFile](tag, "case_file") {

    def caseInstanceId = column[String]("case_instance_id", O.PrimaryKey)

    def tenant = column[String]("tenant")

    def data = column[String]("data")

    def * = (caseInstanceId, tenant, data) <> (CaseFile.tupled, CaseFile.unapply)

    val caseInstanceTable = lifted.TableQuery[CaseInstanceTable]

    def caseInstance = foreignKey("fk_case_file__case_instance", caseInstanceId, caseInstanceTable)(_.id)
  }

  final class CaseInstanceRoleTable(tag: Tag) extends Table[CaseInstanceRole](tag, "case_instance_role") {

    def caseInstanceId = column[String]("case_instance_id")

    def tenant = column[String]("tenant")

    def roleName = column[String]("role_name")

    def assigned = column[Boolean]("assigned") // true if team members are assigned for this role

    def pk = primaryKey("pk_case_instance_role", (caseInstanceId, roleName))

    def * = (caseInstanceId, tenant, roleName, assigned) <> (CaseInstanceRole.tupled, CaseInstanceRole.unapply)

    val caseInstanceTable = lifted.TableQuery[CaseInstanceTable]

    def caseInstance = foreignKey("fk_case_instance_role__case_instance", caseInstanceId, caseInstanceTable)(_.id)
  }

  final class CaseInstanceTeamMemberTable(tag: Tag) extends Table[CaseInstanceTeamMember](tag, "case_instance_team_member") {

    def caseInstanceId = column[String]("case_instance_id")

    def tenant = column[String]("tenant")

    def role = column[String]("role")

    def userId = column[String]("user_id")

    def active = column[Boolean]("active")

    def pk = primaryKey("pk_case_instance_team_member", (caseInstanceId, role, userId))

    def * = (caseInstanceId, tenant, userId, role, active) <> (CaseInstanceTeamMember.tupled, CaseInstanceTeamMember.unapply)

    val caseInstanceTable = lifted.TableQuery[CaseInstanceTable]

    def caseInstance =
      foreignKey("fk_case_instance_team_member__case_instance", caseInstanceId, caseInstanceTable)(_.id)
  }

}
