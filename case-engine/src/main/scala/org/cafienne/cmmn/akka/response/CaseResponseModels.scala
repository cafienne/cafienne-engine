package org.cafienne.cmmn.akka.response

/**
  * Case classes for response payloads of CaseCommands
  */
object CaseResponseModels {
  case class StartCaseResponse(caseInstanceId: String, name: String)

  case class DiscretionaryItem(name: String, definitionId: String, `type`: String, parentName: String, parentType: String, parentId: String)

  case class DiscretionaryItemsList(caseInstanceId: String, discretionaryItems: Seq[DiscretionaryItem])

  case class PlannedDiscretionaryItem(planItemId: String)
}
