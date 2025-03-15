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

package org.cafienne.persistence.querydb.materializer.cases

import com.typesafe.scalalogging.LazyLogging
import org.cafienne.cmmn.actorapi.event.definition.CaseDefinitionEvent
import org.cafienne.cmmn.actorapi.event.migration.CaseDefinitionMigrated
import org.cafienne.cmmn.actorapi.event.{CaseDefinitionApplied, CaseEvent, CaseModified}
import org.cafienne.cmmn.instance.State
import org.cafienne.persistence.querydb.materializer.cases.file.CaseFileProjection
import org.cafienne.persistence.querydb.record.{CaseDefinitionRecord, CaseRecord, CaseRoleRecord}

class CaseProjection(override val batch: CaseEventBatch, caseFileProjection: CaseFileProjection) extends CaseEventMaterializer with LazyLogging {
  private var caseInstance: Option[CaseRecord] = None
  private var caseDefinition: Option[CaseDefinitionRecord] = None

  def handleCaseEvent(event: CaseEvent): Unit = {
    event match {
      case event: CaseDefinitionApplied => createCaseInstance(event)
      case event: CaseDefinitionMigrated => migrateCaseDefinition(event)
      case event: CaseModified => updateCaseModified(event)
      case _ => // Ignore other events
    }
  }

  private def createCaseInstance(event: CaseDefinitionApplied): Unit = {
    upsertCaseDefinitionRecords(event)
    this.caseInstance = Some(CaseRecord(
      id = event.getCaseInstanceId,
      tenant = event.tenant,
      rootCaseId = event.getRootCaseId,
      parentCaseId = event.getParentCaseId,
      caseName = event.getCaseName,
      state = State.Active.toString, // Will always be overridden from CaseModified event
      failures = 0,
      lastModified = event.createdOn,
      modifiedBy = event.createdBy,
      createdBy = event.createdBy,
      createdOn = event.createdOn
    ))
    caseFileProjection.handleCaseCreation()
  }

  private def upsertCaseDefinitionRecords(event: CaseDefinitionEvent): Unit = {
    import scala.jdk.CollectionConverters._

    // First upsert the CaseDefinition, then all roles
    caseDefinition = Some(CaseDefinitionRecord(event.getActorId, event.getCaseName, event.getDefinition.documentation.text, event.getDefinition.getId, event.getDefinition.getDefinitionsDocument.getSource, event.tenant, event.getTimestamp, event.getUser.id))
    val roles = event.getDefinition.getCaseTeamModel.getCaseRoles.asScala.toSeq
    roles.foreach(role => dBTransaction.upsert(CaseRoleRecord(event.getCaseInstanceId, event.tenant, role.getName, assigned = false)))
  }

  private def migrateCaseDefinition(event: CaseDefinitionMigrated): Unit = {
    // Remove existing roles
    dBTransaction.removeCaseRoles(event.getCaseInstanceId)
    // Upsert case definition will add the new roles
    upsertCaseDefinitionRecords(event)
    changeCaseRecord(event, instance => instance.copy(caseName = event.getDefinition.getName))
  }

  private def updateCaseModified(evt: CaseModified): Unit = {
    caseDefinition.foreach(definition => caseDefinition = Some(definition.copy(lastModified = evt.lastModified)))
    changeCaseRecord(evt, instance => instance.copy(lastModified = evt.lastModified, modifiedBy = evt.getUser.id, failures = evt.getNumFailures, state = evt.getState.toString))
  }

  private def changeCaseRecord(event: CaseEvent, changer: CaseRecord => CaseRecord): Unit = {
    val caseInstanceId = event.getCaseInstanceId
    caseInstance match {
      case None =>
        logger.whenDebugEnabled(logger.debug(s"Retrieving Case[$caseInstanceId] from database"))
        dBTransaction.getCaseInstance(caseInstanceId) match  {
          case Some(instance) =>
            this.caseInstance = Some(changer(instance))
          case None =>
            logger.error(s"Cannot find Case[$caseInstanceId] in database to handle ${event.getClass.getSimpleName}.\nEventBatch has ${batch.events.size} events:\n- ${batch.events.map(e => e.event.getDescription).mkString("\n- ")}\n=============\n")
          }
      case Some(instance) =>
        logger.whenDebugEnabled(logger.debug(s"Found Case[$caseInstanceId] in current transaction cache"))
        this.caseInstance = Some(changer(instance))
    }
  }

  def prepareCommit(): Unit = {
    this.caseInstance.foreach(instance => dBTransaction.upsert(instance))
    this.caseDefinition.foreach(instance => dBTransaction.upsert(instance))
  }
}
