package org.cafienne.service.db.record

import org.cafienne.json.{JSONReader, Value, ValueMap}

import java.time.Instant
import org.cafienne.json.JSONReader
import org.cafienne.json.CafienneJson

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

  def getJSON(value: String): Value[_] = if (value == "" || value == null) new ValueMap else JSONReader.parse(value)

  override def toValue: Value[_] = {
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
