package org.cafienne.service.db.schema.table

import org.cafienne.service.db.record.TaskRecord
import org.cafienne.service.db.schema.QueryDBSchema
import slick.lifted.ColumnOrdered

import java.time.Instant

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

    lazy val id = idColumn[String]("id", O.PrimaryKey)

    lazy val caseInstanceId = idColumn[String]("case_instance_id")

    lazy val role = column[String]("role", O.Default(""))

    lazy val taskName = column[String]("task_name", O.Default(""))

    lazy val taskState = stateColumn[String]("task_state", O.Default(""))

    lazy val assignee = userColumn[String]("assignee", O.Default(""))

    lazy val owner = userColumn[String]("owner", O.Default(""))

    lazy val dueDate = column[Option[Instant]]("due_date")

    lazy val createdOn = column[Instant]("created_on")

    lazy val createdBy = userColumn[String]("created_by", O.Default(""))

    lazy val lastModified = column[Instant]("last_modified")

    lazy val modifiedBy = userColumn[String]("modified_by", O.Default(""))

    lazy val input = jsonColumn[String]("task_input", O.Default(""))

    lazy val output = jsonColumn[String]("task_output", O.Default(""))

    lazy val taskModel = jsonColumn[String]("task_model", O.Default(""))

    // Various indices for optimizing getAllTasks queries
    lazy val indexCaseInstanceId = oldStyleIndex(caseInstanceId)
    lazy val indexTaskState = oldStyleIndex(taskState)
    lazy val indexAssignee = oldStyleIndex(assignee)
    lazy val indexOwner = oldStyleIndex(owner)
    lazy val indexCreatedBy = oldStyleIndex(createdBy)
    lazy val indexModifiedBy = oldStyleIndex(modifiedBy)
    lazy val indexTenant = oldStyleIndex(tenant)
    lazy val indexDueDate = index(oldStyleIxName(dueDate), dueDate)

    lazy val * = (id, caseInstanceId, tenant, taskName, taskState, role, assignee, owner, dueDate, createdOn, createdBy, lastModified, modifiedBy, input, output, taskModel).mapTo[TaskRecord]
  }
}
