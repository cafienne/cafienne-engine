package org.cafienne.service.api.projection.record

import java.time.Instant

import org.cafienne.cmmn.instance.casefile.{JSONReader, Value, ValueList, ValueMap}
import org.cafienne.infrastructure.json.CafienneJson

final case class CaseRecord(id: String,
                            tenant: String,
                            caseName: String,
                            state: String,
                            failures: Int,
                            parentCaseId: String = "",
                            rootCaseId: String,
                            lastModified: Instant,
                            modifiedBy: String = "",
                            createdOn: Instant,
                            createdBy: String = "",
                            caseInput: String = "",
                            caseOutput: String = "") extends CafienneJson {

  override def toValue: Value[_] = {
    val v = new ValueMap
    v.putRaw("id", id)
    v.putRaw("tenant", tenant)
    v.putRaw("caseName", caseName)
    v.putRaw("definition", caseName) // Deprecated field
    v.putRaw("name", caseName) // Deprecated field - todo: check where it must be removed (generic-ui probably)
    v.putRaw("state", state)
    v.putRaw("failures", failures)
    v.putRaw("parentCaseId", parentCaseId)
    v.putRaw("rootCaseId", rootCaseId)
    v.putRaw("createdOn", createdOn)
    v.putRaw("createdBy", createdBy)
    v.putRaw("lastModified", lastModified)
    v.putRaw("modifiedBy", modifiedBy)
    v.putRaw("modifiedBy", modifiedBy)
    v.putRaw("caseInput", "")
    v.putRaw("caseOutput", "")
    // Adding default empty values for case plan, case team and case file.
    v.putRaw("planitems", new ValueList())
    v.putRaw("team", new ValueList())
    v.putRaw("file", new ValueMap())
    v
  }
}

final case class CaseDefinitionRecord(caseInstanceId: String, name: String, description: String, elementId: String, content: String, tenant: String, lastModified: Instant, modifiedBy: String)

final case class CaseFileRecord(caseInstanceId: String, tenant: String, data: String) {
  def toValueMap: ValueMap = JSONReader.parse(data)
}

final case class CaseBusinessIdentifierRecord(caseInstanceId: String, tenant: String, name: String, value: Option[String], active: Boolean, path: String)

final case class CaseRoleRecord(caseInstanceId: String, tenant: String, roleName: String, assigned: Boolean = true)

final case class CaseTeamMemberRecord(caseInstanceId: String, tenant: String, memberId: String, caseRole: String, isTenantUser: Boolean, isOwner: Boolean, active: Boolean)

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
                                       rawOutput: String = "") extends CafienneJson {

  override def toValue: ValueMap = {
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
