/*
 * Copyright (C) 2014  Batav B.V.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.cafienne.persistence.querydb.record

import org.cafienne.cmmn.definition.{CaseDefinition, DefinitionsDocument}
import org.cafienne.json._

import java.time.Instant

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
    v.plus("id", id)
    v.plus("tenant", tenant)
    v.plus("caseName", caseName)
    v.plus("definition", caseName) // Deprecated field
    v.plus("name", caseName) // Deprecated field - todo: check where it must be removed (generic-ui probably)
    v.plus("state", state)
    v.plus("failures", failures)
    v.plus("parentCaseId", parentCaseId)
    v.plus("rootCaseId", rootCaseId)
    v.plus("createdOn", createdOn)
    v.plus("createdBy", createdBy)
    v.plus("lastModified", lastModified)
    v.plus("modifiedBy", modifiedBy)
    v.plus("modifiedBy", modifiedBy)
    v.plus("caseInput", "")
    v.plus("caseOutput", "")
    // Adding default empty values for case plan, case team and case file.
    v.plus("planitems", new ValueList())
    v.plus("team", new ValueList())
    v.plus("file", new ValueMap())
    v
  }
}

final case class CaseDefinitionRecord(caseInstanceId: String, name: String, description: String, elementId: String, content: String, tenant: String, lastModified: Instant, modifiedBy: String) {
  lazy val definitions: DefinitionsDocument = DefinitionsDocument.fromSource(content)
  lazy val caseDefinition: CaseDefinition = definitions.getCaseDefinition(elementId)
}

final case class CaseFileRecord(caseInstanceId: String, tenant: String, data: String) extends CafienneJson {
  override def toValue: ValueMap = JSONReader.parse(data)
}

final case class CaseBusinessIdentifierRecord(caseInstanceId: String, tenant: String, name: String, value: Option[String], active: Boolean, path: String) extends CafienneJson {
  override def toValue: Value[_] = new ValueMap("name", name, "value", value.orNull)
}

final case class CaseRoleRecord(caseInstanceId: String, tenant: String, roleName: String, assigned: Boolean = true)

final case class CaseTeamUserRecord(caseInstanceId: String, tenant: String, userId: String, origin: String, caseRole: String, isOwner: Boolean)

final case class CaseTeamTenantRoleRecord(caseInstanceId: String, tenant: String, tenantRole: String, caseRole: String, isOwner: Boolean)

final case class CaseTeamGroupRecord(caseInstanceId: String, tenant: String, groupId: String, groupRole: String, caseRole: String, isOwner: Boolean)

final case class PlanItemRecord(id: String,
                                definitionId: String,
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
                                rawOutput: String = "") extends CafienneJson {

  def toValue: ValueMap = {
    val v = new ValueMap
    v.plus("id", id)
    v.plus("caseInstanceId", caseInstanceId)
    v.plus("definitionId", definitionId)
    v.plus("stageId", stageId)
    v.plus("name", name)
    v.plus("index", index)
    v.plus("currentState", currentState)
    v.plus("historyState", historyState)
    v.plus("isRequired", required)
    v.plus("isRepeating", repeating)
    v.plus("type", planItemType)
    v.plus("transition", transition)
    v.plus("lastModified", lastModified)
    v.plus("modifiedBy", modifiedBy)
    v.plus("user", modifiedBy) // Temporary compatibility
    v
  }

}

final case class PlanItemHistoryRecord(id: String = "",
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
                                       sequenceNr: Long = 0,
                                       taskInput: String = "",
                                       taskOutput: String = "",
                                       mappedInput: String = "",
                                       rawOutput: String = "") extends CafienneJson {

  override def toValue: ValueMap = {
    val v = new ValueMap
    v.plus("id", id)
    v.plus("planItemId", id)
    v.plus("name", name)
    v.plus("index", index)
    v.plus("stageId", stageId)
    v.plus("caseInstanceId", caseInstanceId)
    v.plus("currentState", currentState)
    v.plus("historyState", historyState)
    v.plus("type", planItemType)
    v.plus("transition", transition)
    v.plus("repeating", repeating)
    v.plus("required", required)
    v.plus("lastModified", lastModified)
    v.plus("modifiedBy", modifiedBy)
    v.plus("eventType", eventType)
    v.plus("sequenceNr", sequenceNr)
    v
  }

}
