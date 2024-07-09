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

package org.cafienne.querydb.materializer.cases.file

import org.apache.pekko.Done
import com.typesafe.scalalogging.LazyLogging
import org.cafienne.cmmn.actorapi.event.file._
import org.cafienne.cmmn.actorapi.event.migration.{CaseFileItemDropped, CaseFileItemMigrated}
import org.cafienne.json.{JSONReader, ValueMap}
import org.cafienne.querydb.materializer.cases.{CaseEventBatch, CaseStorageTransaction}
import org.cafienne.querydb.record.{CaseBusinessIdentifierRecord, CaseFileRecord}

import scala.concurrent.{ExecutionContext, Future}

class CaseFileProjection(batch: CaseEventBatch)(implicit val executionContext: ExecutionContext) extends LazyLogging {
  lazy val dBTransaction: CaseStorageTransaction = batch.dBTransaction
  lazy val caseInstanceId: String = batch.caseInstanceId
  lazy val tenant: String = batch.tenant

  private val businessIdentifiers = scala.collection.mutable.Set[CaseBusinessIdentifierRecord]()
  private val bufferedCaseFileEvents = new CaseFileEventBuffer()
  private var caseFile: Option[ValueMap] = None

  def handleCaseCreation(): Unit = setCaseFile(new ValueMap()) // Always create an empty case file

  def handleCaseFileEvent(event: CaseFileEvent): Future[Done] = {
    event match {
      case itemEvent: CaseFileItemTransitioned => handleCaseFileItemEvent(itemEvent)
      case identifierEvent: BusinessIdentifierEvent => handleBusinessIdentifierEvent(identifierEvent)
      case migrated: CaseFileItemMigrated => handleCaseFileMigration(migrated)
      case dropped: CaseFileItemDropped => handleCaseFileDropped(dropped)
      case _ => Future.successful(Done) // Ignore other events
    }
  }

  private def handleBusinessIdentifierEvent(event: BusinessIdentifierEvent): Future[Done] = {
    event match {
      case event: BusinessIdentifierSet => businessIdentifiers.add(CaseIdentifierMerger.merge(event))
      case event: BusinessIdentifierCleared => businessIdentifiers.add(CaseIdentifierMerger.merge(event))
      case _ => // Ignore other events
    }
    Future.successful(Done)
  }

  private def handleCaseFileItemEvent(event: CaseFileItemTransitioned): Future[Done] = {
    bufferedCaseFileEvents.addEvent(event)
    // Fetch the existing case file data, so that we can apply the events to it later on
    getCaseFile(caseInstanceId).map(_ => Done)
  }

  private def handleCaseFileMigration(event: CaseFileItemMigrated): Future[Done] = {
    // Fetch the existing case file data, and then change the existing path to which the event is pointing into the new one
    getCaseFile(caseInstanceId).map(json => {
      val parent = event.formerPath.resolveParent(json)
      parent.put(event.path.name, parent.get(event.formerPath.name))
      parent.getValue.remove(event.formerPath.name)
    })
    Future.successful(Done)
  }

  private def handleCaseFileDropped(event: CaseFileItemDropped): Future[Done] = {
    // Fetch the existing case file data, and then change the existing path to which the event is pointing into the new one
    getCaseFile(caseInstanceId).map(json => {
      val parent = event.path.resolveParent(json)
      parent.getValue.remove(event.path.name)
    })
    Future.successful(Done)
  }

  private def setCaseFile(data: ValueMap): ValueMap = {
    this.caseFile = Some(data)
    data
  }

  private def getCaseFile(caseInstanceId: String): Future[ValueMap] = {
    if (this.caseFile.isEmpty) {
      logger.whenDebugEnabled(logger.debug("Retrieving casefile caseInstanceId={} from database", caseInstanceId))
      dBTransaction.getCaseFile(caseInstanceId).map {
        case Some(record) => JSONReader.parse(record.data)
        case None => new ValueMap()
      }.map(setCaseFile)
    } else {
      Future.successful(this.caseFile.get)
    }
  }

  def prepareCommit(): Unit = {
    // Update case file and identifiers
    this.caseFile.map(getUpdatedCaseFile).foreach(caseFile => dBTransaction.upsert(caseFile))
    this.businessIdentifiers.toSeq.foreach(item => dBTransaction.upsert(item))
  }

  /**
    * Depending on the presence of CaseFileEvents this will add a new CaseFileRecord
    */
  private def getUpdatedCaseFile(caseFileInProgress: ValueMap): CaseFileRecord = {
    bufferedCaseFileEvents.update(caseFileInProgress)
    CaseFileRecord(caseInstanceId, tenant, caseFileInProgress.toString)
  }
}
