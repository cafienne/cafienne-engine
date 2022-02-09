package org.cafienne.querydb.record

import org.cafienne.json._

import java.time.Instant

final case class TaskRecord(id: String,
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
                     ) extends CafienneJson {

  def getJSON(value: String): Value[_] = {
    if (value == "" || value == null) new ValueMap else {
      try {
        JSONReader.parse(value)
      } catch {
        case _: Throwable => new StringValue(value)
      }
    }
  }

  override def toValue: Value[_] = {
    val v = new ValueMap
    v.plus("id", id)
    v.plus("taskName", taskName)
    v.plus("taskState", taskState)
    v.plus("assignee", assignee)
    v.plus("owner", owner)
    v.plus("tenant", tenant)
    v.plus("caseInstanceId", caseInstanceId)
    v.plus("role", role)
    v.plus("lastModified", lastModified)
    v.plus("modifiedBy", modifiedBy)
    v.plus("dueDate", dueDate.getOrElse(""))
    v.plus("createdOn", createdOn)
    v.plus("createdBy", createdBy)
    v.plus("input", getJSON(input))
    v.plus("output", getJSON(output))
    v.plus("taskModel", getJSON(taskModel))
    v
  }
}
