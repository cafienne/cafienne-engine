package org.cafienne.service.api.tasks.model

import io.swagger.v3.oas.annotations.media.Schema
import org.cafienne.infrastructure.akkahttp.EntityReader.{EntityReader, entityReader}

import scala.annotation.meta.field

object TaskAPI {

  implicit val assigneeReader: EntityReader[Assignee] = entityReader[Assignee]

  @Schema(description = "Assign a task to someone")
  case class Assignee(@(Schema@field)(description = "Assignee", required = true, implementation = classOf[String]) assignee: String)

  object Examples {
    @Schema(description = "Output parameters example json")
    case class TaskOutputFormat(output1: String, output2: Object, output3: List[String])
  }
}
