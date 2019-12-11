package org.cafienne.service.api.tasks

import java.time.Instant

import org.cafienne.cmmn.instance.casefile.{JSONReader, Value, ValueMap}
import org.cafienne.infrastructure.jdbc.DbConfig

final case class Task(id: String,
                      caseInstanceId: String,
                      tenant: String,
                      taskName: String = "",
                      taskState: String = "",
                      role: String = "",
                      assignee: String = "",
                      owner: String = "",
                      dueDate: Option[Instant] = None,
                      createdOn: Instant,
                      createdBy: String = "",
                      lastModified: Instant,
                      modifiedBy: String = "",
                      input: String = "",
                      output: String = "",
                      taskModel: String = ""
                     ) {

  def getJSON(value: String): Value[_] = if (value == "" || value == null) new ValueMap else JSONReader.parse(value)

  def toValueMap: ValueMap = {
    val v = new ValueMap
    v.putRaw("id", id)
    v.putRaw("taskName", taskName)
    v.putRaw("taskState", taskState)
    v.putRaw("assignee", assignee)
    v.putRaw("owner", owner)
    v.putRaw("tenant", tenant)
    v.putRaw("caseInstanceId", caseInstanceId)
    v.putRaw("role", role)
    v.putRaw("lastModified", lastModified)
    v.putRaw("modifiedBy", modifiedBy)
    v.putRaw("dueDate", dueDate.getOrElse(""))
    v.putRaw("createdOn", createdOn)
    v.putRaw("createdBy", createdBy)
    v.putRaw("input", getJSON(input))
    v.putRaw("output", getJSON(output))
    v.putRaw("taskModel", getJSON(taskModel))
    v
  }
}

trait TaskTables extends DbConfig {

  import dbConfig.profile.api._

  // Schema for the "task" table:
  final class TaskTable(tag: Tag) extends Table[Task](tag, "task") {

    def id = column[String]("id", O.PrimaryKey)

    def caseInstanceId = column[String]("case_instance_id")

    def tenant = column[String]("tenant")

    def role = column[String]("role", O.Default(""))

    def taskName = column[String]("task_name", O.Default(""))

    def taskState = column[String]("task_state", O.Default(""))

    def assignee = column[String]("assignee", O.Default(""))

    def owner = column[String]("owner", O.Default(""))

    def dueDate = column[Option[Instant]]("due_date")

    def createdOn = column[Instant]("created_on")

    def createdBy = column[String]("created_by", O.Default(""))

    def lastModified = column[Instant]("last_modified")

    def modifiedBy = column[String]("modified_by", O.Default(""))

    def input = column[String]("task_input", O.Default(""))

    def output = column[String]("task_output", O.Default(""))

    def taskModel = column[String]("task_model", O.Default(""))

    def * = (id, caseInstanceId, tenant, taskName, taskState, role, assignee, owner, dueDate, createdOn, createdBy, lastModified, modifiedBy, input, output, taskModel).mapTo[Task]
  }
}