package org.cafienne.service.api.tasks

import java.time.Instant

import org.cafienne.infrastructure.jdbc.QueryDbConfig

trait TaskTables extends QueryDbConfig {

  import dbConfig.profile.api._

  // Schema for the "task" table:
  final class TaskTable(tag: Tag) extends CafienneTable[TaskRecord](tag, "task") {

    def id = idColumn[String]("id", O.PrimaryKey)

    def caseInstanceId = idColumn[String]("case_instance_id")

    def tenant = idColumn[String]("tenant")

    def role = column[String]("role", O.Default(""))

    def taskName = column[String]("task_name", O.Default(""))

    def taskState = stateColumn[String]("task_state", O.Default(""))

    def assignee = userColumn[String]("assignee", O.Default(""))

    def owner = userColumn[String]("owner", O.Default(""))

    def dueDate = column[Option[Instant]]("due_date")

    def createdOn = column[Instant]("created_on")

    def createdBy = userColumn[String]("created_by", O.Default(""))

    def lastModified = column[Instant]("last_modified")

    def modifiedBy = userColumn[String]("modified_by", O.Default(""))

    def input = jsonColumn[String]("task_input", O.Default(""))

    def output = jsonColumn[String]("task_output", O.Default(""))

    def taskModel = jsonColumn[String]("task_model", O.Default(""))

    // Various indices for optimizing getAllTasks queries
    def indexCaseInstanceId = index(caseInstanceId)
    def indexTaskState = index(taskState)
    def indexAssignee = index(assignee)
    def indexTenant = index(tenant)
    def indexDueDate = index(generateIndexName(dueDate), dueDate)

    def * = (id, caseInstanceId, tenant, taskName, taskState, role, assignee, owner, dueDate, createdOn, createdBy, lastModified, modifiedBy, input, output, taskModel).mapTo[TaskRecord]
  }

  final class TaskTeamMemberTable(tag: Tag) extends CafienneTable[TaskTeamMemberRecord](tag, "task_team_member") {

    def caseInstanceId = idColumn[String]("case_instance_id")

    def tenant = idColumn[String]("tenant")

    def caseRole = idColumn[String]("case_role")

    def memberId = idColumn[String]("member_id")

    def isTenantUser = column[Boolean]("isTenantUser")

    def isOwner = column[Boolean]("isOwner")

    def active = column[Boolean]("active")

    def pk = primaryKey("pk_task_team_member", (caseInstanceId, caseRole, memberId, isTenantUser))

    def * = (caseInstanceId, tenant, memberId, caseRole, isTenantUser, isOwner, active) <> (TaskTeamMemberRecord.tupled, TaskTeamMemberRecord.unapply)
  }
}
