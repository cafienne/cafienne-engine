package org.cafienne.service.api.cases.table

import java.time.Instant

import org.cafienne.cmmn.instance.casefile.{JSONReader, ValueMap}

final case class CaseRecord(id: String,
                            tenant: String,
                            name: String,
                            state: String,
                            failures: Int,
                            parentCaseId: String = "",
                            rootCaseId: String,
                            lastModified: Instant,
                            modifiedBy: String = "",
                            createdOn: Instant,
                            createdBy: String = "",
                            caseInput: String = "",
                            caseOutput: String = "") {

  def toValueMap: ValueMap = {
    val v = new ValueMap
    v.putRaw("id", id)
    v.putRaw("tenant", tenant)
    v.putRaw("definition", name)
    v.putRaw("name", name)
    v.putRaw("state", state)
    v.putRaw("failures", failures)
    v.putRaw("parentCaseId", parentCaseId)
    v.putRaw("rootCaseId", rootCaseId)
    v.putRaw("createdOn", createdOn)
    v.putRaw("createdBy", createdBy)
    v.putRaw("lastModified", lastModified)
    v.putRaw("modifiedBy", modifiedBy)
    v.putRaw("caseInput", "")
    v.putRaw("caseOutput", "")
    v
  }
}

final case class CaseDefinitionRecord(caseInstanceId: String, name: String, description: String, elementId: String, content: String, tenant: String, lastModified: Instant, modifiedBy: String)

final case class CaseFileRecord(caseInstanceId: String, tenant: String, data: String) {
  def toValueMap: ValueMap = JSONReader.parse(data)
}

final case class CaseRoleRecord(caseInstanceId: String, tenant: String, roleName: String, assigned: Boolean = true)

final case class CaseTeamMemberRecord(caseInstanceId: String, tenant: String, userId: String, role: String, active: Boolean)

final case class PlanItemRecord(id: String,
                                stageId: String,
                                name: String,
                                index: Int,
                                caseInstanceId: String,
                                tenant: String,
                                currentState: String = "",
                                historyState: String = "",
                                transition: String = "",
                                planItemType: String,
                                repeating: Boolean = false,
                                required: Boolean = false,
                                lastModified: Instant,
                                modifiedBy: String,
                                createdOn: Instant,
                                createdBy: String = "",
                                taskInput: String = "",
                                taskOutput: String = "",
                                mappedInput: String = "",
                                rawOutput: String = "") {

  def toValueMap: ValueMap = {
    val v = new ValueMap
    v.putRaw("isRequired", required)
    v.putRaw("isRepeating", repeating)
    v.putRaw("caseInstanceId", caseInstanceId)
    v.putRaw("id", id)
    v.putRaw("name", name)
    v.putRaw("index", index)
    v.putRaw("stageId", stageId)
    v.putRaw("currentState", currentState)
    v.putRaw("historyState", historyState)
    v.putRaw("type", planItemType)
    v.putRaw("transition", transition)
    v.putRaw("lastModified", lastModified)
    v.putRaw("modifiedBy", modifiedBy)
    v.putRaw("user", modifiedBy) // Temporary compatibility
    v
  }

}

final case class PlanItemHistoryRecord(id: String,
                                       planItemId: String,
                                       stageId: String = "",
                                       name: String = "",
                                       index: Int,
                                       caseInstanceId: String,
                                       tenant: String,
                                       currentState: String = "",
                                       historyState: String = "",
                                       transition: String = "",
                                       planItemType: String = "",
                                       repeating: Boolean = false,
                                       required: Boolean = false,
                                       lastModified: Instant,
                                       modifiedBy: String,
                                       eventType: String,
                                       sequenceNr: Long,
                                       taskInput: String = "",
                                       taskOutput: String = "",
                                       mappedInput: String = "",
                                       rawOutput: String = "") {



  def toValueMap: ValueMap = {
    val v = new ValueMap
    v.putRaw("id", id)
    v.putRaw("planItemId", id)
    v.putRaw("name", name)
    v.putRaw("index", index)
    v.putRaw("stageId", stageId)
    v.putRaw("caseInstanceId", caseInstanceId)
    v.putRaw("currentState", currentState)
    v.putRaw("historyState", historyState)
    v.putRaw("type", planItemType)
    v.putRaw("transition", transition)
    v.putRaw("repeating", repeating)
    v.putRaw("required", required)
    v.putRaw("lastModified", lastModified)
    v.putRaw("modifiedBy", modifiedBy)
    v.putRaw("eventType", eventType)
    v.putRaw("sequenceNr", sequenceNr)
    v
  }

}
