package org.cafienne.service.db.schema.table

import java.time.Instant

import org.cafienne.service.db.record.TaskRecord
import org.cafienne.service.db.schema.QueryDBSchema
import slick.lifted.ColumnOrdered

trait TaskTables extends QueryDBSchema {

  import dbConfig.profile.api._

  // Schema for the "task" table:
  final class TaskTable(tag: Tag) extends CafienneTenantTable[TaskRecord](tag, "task") {

    override def getSortColumn(field: String): ColumnOrdered[_] = field match {
      case "taskstate" => taskState
      case "assignee" => assignee
      case "owner" => owner
      case "duedate" => dueDate
      case "createdon" => createdOn
      case "createdby" => createdBy
      case "modifiedby" => modifiedBy
      case "lastmodified" => lastModified
      case _ => lastModified
    }

    def id = idColumn[String]("id", O.PrimaryKey)

    def caseInstanceId = idColumn[String]("case_instance_id")

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
    def indexOwner = index(owner)
    def indexCreatedBy = index(createdBy)
    def indexModifiedBy = index(modifiedBy)
    def indexTenant = index(tenant)
    def indexDueDate = index(generateIndexName(dueDate), dueDate)

    def * = (id, caseInstanceId, tenant, taskName, taskState, role, assignee, owner, dueDate, createdOn, createdBy, lastModified, modifiedBy, input, output, taskModel).mapTo[TaskRecord]
  }
}
