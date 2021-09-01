package org.cafienne.service.db.materializer.cases

import akka.Done
import akka.persistence.query.Offset
import com.typesafe.scalalogging.LazyLogging
import org.cafienne.cmmn.actorapi.event.{CaseAppliedPlatformUpdate, CaseDefinitionApplied, CaseEvent, CaseModified}
import org.cafienne.cmmn.instance.State
import org.cafienne.service.db.materializer.RecordsPersistence
import org.cafienne.service.db.materializer.cases.file.CaseFileProjection
import org.cafienne.service.db.record.{CaseDefinitionRecord, CaseRecord, CaseRoleRecord}

import scala.concurrent.{ExecutionContext, Future}

class CaseProjection(persistence: RecordsPersistence, caseFileProjection: CaseFileProjection)(implicit val executionContext: ExecutionContext) extends LazyLogging {
  private var caseInstance: Option[CaseRecord] = None
  private var caseDefinition: Option[CaseDefinitionRecord] = None

  def handleCaseEvent(event: CaseEvent, offsetName: String, offset: Offset): Future[Done] = {
    event match {
      case event: CaseDefinitionApplied => createCaseInstance(event)
      case event: CaseModified => updateCaseModified(event)
      case event: CaseAppliedPlatformUpdate => persistence.updateCaseUserInformation(event.getCaseInstanceId, event.newUserInformation.info, offsetName, offset)
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

  private def upsertCaseDefinitionRecords(event: CaseDefinitionApplied): Unit = {
    import scala.jdk.CollectionConverters._

    // First upsert the CaseDefinition, then all roles
    caseDefinition = Some(CaseDefinitionRecord(event.getActorId, event.getCaseName, event.getDefinition.documentation.text, event.getDefinition.getId, event.getDefinition.getDefinitionsDocument.getSource, event.tenant, event.getTimestamp, event.getUser.id))
    val roles = event.getDefinition.getCaseTeamModel.getCaseRoles.asScala.toSeq
    roles.foreach(role => persistence.upsert(CaseRoleRecord(event.getCaseInstanceId, event.tenant, role.getName, assigned = false)))
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
        persistence.getCaseInstance(caseInstanceId).map {
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
    this.caseInstance.foreach(instance => persistence.upsert(instance))
    this.caseDefinition.foreach(instance => persistence.upsert(instance))
  }
}
