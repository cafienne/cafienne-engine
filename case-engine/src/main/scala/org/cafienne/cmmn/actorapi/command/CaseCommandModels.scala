package org.cafienne.cmmn.actorapi.command

import io.swagger.v3.oas.annotations.media.Schema

import scala.annotation.meta.field

/**
  * Case classes for request payloads of CaseCommands
  */
object CaseCommandModels {

  case class PlanDiscretionaryItem(name: String, definitionId: String, parentId: String, planItemId: Option[String])

  @Schema(description = "Assign a task to someone")
  case class Assignee(
                       @(Schema @field)(description = "Assignee", required = true, implementation = classOf[String]) assignee: String
                     )

}
