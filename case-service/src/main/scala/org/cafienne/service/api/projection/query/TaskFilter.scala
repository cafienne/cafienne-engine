package org.cafienne.service.api.projection.query

case class TaskFilter(tenant: Option[String],
                      identifiers: Option[String],
                      caseName: Option[String],
                      taskName: Option[String],
                      taskState: Option[String],
                      assignee: Option[String],
                      owner: Option[String],
                      dueOn: Option[String],
                      dueBefore: Option[String],
                      dueAfter: Option[String],
                      timeZone: Option[String])

object TaskFilter {
  val Empty = TaskFilter(None, None, None, None, None, None, None, None, None, None, None)
}
