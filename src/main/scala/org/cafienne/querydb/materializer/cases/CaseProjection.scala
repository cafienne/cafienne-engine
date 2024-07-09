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

package org.cafienne.querydb.materializer.cases

import org.apache.pekko.Done
import com.typesafe.scalalogging.LazyLogging
import org.cafienne.cmmn.actorapi.event.definition.CaseDefinitionEvent
import org.cafienne.cmmn.actorapi.event.migration.CaseDefinitionMigrated
import org.cafienne.cmmn.actorapi.event.{CaseDefinitionApplied, CaseEvent, CaseModified}
import org.cafienne.cmmn.instance.State
import org.cafienne.querydb.materializer.cases.file.CaseFileProjection
import org.cafienne.querydb.record.{CaseDefinitionRecord, CaseRecord, CaseRoleRecord}

import scala.concurrent.{ExecutionContext, Future}

class CaseProjection(override val batch: CaseEventBatch, caseFileProjection: CaseFileProjection)(implicit val executionContext: ExecutionContext) extends CaseEventMaterializer with LazyLogging {
  private var caseInstance: Option[CaseRecord] = None
  private var caseDefinition: Option[CaseDefinitionRecord] = None

  def handleCaseEvent(event: CaseEvent): Future[Done] = {
    event match {
      case event: CaseDefinitionApplied => createCaseInstance(event)
      case event: CaseDefinitionMigrated => migrateCaseDefinition(event)
      case event: CaseModified => updateCaseModified(event)
      case _ => Future.successful(Done) // Ignore other events
    }
  }

  private def createCaseInstance(event: CaseDefinitionApplied): Future[Done] = {
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
    Future.successful(Done)
  }

  private def upsertCaseDefinitionRecords(event: CaseDefinitionEvent): Unit = {
    import scala.jdk.CollectionConverters._

    // First upsert the CaseDefinition, then all roles
    caseDefinition = Some(CaseDefinitionRecord(event.getActorId, event.getCaseName, event.getDefinition.documentation.text, event.getDefinition.getId, event.getDefinition.getDefinitionsDocument.getSource, event.tenant, event.getTimestamp, event.getUser.id))
    val roles = event.getDefinition.getCaseTeamModel.getCaseRoles.asScala.toSeq
    roles.foreach(role => dBTransaction.upsert(CaseRoleRecord(event.getCaseInstanceId, event.tenant, role.getName, assigned = false)))
  }

  private def migrateCaseDefinition(event: CaseDefinitionMigrated): Future[Done] = {
    // Remove existing roles
    dBTransaction.removeCaseRoles(event.getCaseInstanceId)
    // Upsert case definition will add the new roles
    upsertCaseDefinitionRecords(event)
    changeCaseRecord(event, instance => instance.copy(caseName = event.getDefinition.getName))
  }

  private def updateCaseModified(evt: CaseModified): Future[Done] = {
    caseDefinition.foreach(definition => caseDefinition = Some(definition.copy(lastModified = evt.lastModified)))
    changeCaseRecord(evt, instance => instance.copy(lastModified = evt.lastModified, modifiedBy = evt.getUser.id, failures = evt.getNumFailures, state = evt.getState.toString))
  }

  private def changeCaseRecord(event: CaseEvent, changer: CaseRecord => CaseRecord): Future[Done] = {
    val caseInstanceId = event.getCaseInstanceId
    caseInstance match {
      case None =>
        logger.whenDebugEnabled(logger.debug(s"Retrieving Case[$caseInstanceId] from database"))
        dBTransaction.getCaseInstance(caseInstanceId).map {
          case Some(instance) =>
            this.caseInstance = Some(changer(instance))
            Done
          case None =>
            logger.error(s"Cannot find Case[$caseInstanceId] in database to handle ${event.getClass.getName}:\n\n" + event.toString + "\n\n")
            Done
        }
      case Some(instance) =>
        logger.whenDebugEnabled(logger.debug(s"Found Case[$caseInstanceId] in current transaction cache"))
        this.caseInstance = Some(changer(instance))
        Future.successful(Done)
    }
  }

  def prepareCommit(): Unit = {
    this.caseInstance.foreach(instance => dBTransaction.upsert(instance))
    this.caseDefinition.foreach(instance => dBTransaction.upsert(instance))
  }
}
