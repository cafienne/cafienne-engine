package org.cafienne.service.api.projection.cases

import akka.Done
import akka.persistence.query.Offset
import com.typesafe.scalalogging.LazyLogging
import org.cafienne.akka.actor.identity.TenantUser
import org.cafienne.cmmn.akka.event.team.{CaseTeamEvent, TeamMemberAdded, TeamMemberRemoved}
import org.cafienne.cmmn.akka.event._
import org.cafienne.cmmn.akka.event.file.CaseFileEvent
import org.cafienne.cmmn.akka.event.plan.{PlanItemCreated, PlanItemEvent, PlanItemTransitioned, RepetitionRuleEvaluated, RequiredRuleEvaluated}
import org.cafienne.cmmn.instance.casefile.{JSONReader, ValueMap}
import org.cafienne.infrastructure.cqrs.NamedOffset
import org.cafienne.service.api.cases._
import org.cafienne.service.api.projection.RecordsPersistence

import scala.collection.mutable.ListBuffer
import scala.concurrent.{ExecutionContext, Future}

class CaseTransaction(caseInstanceId: String, tenant: String, persistence: RecordsPersistence)(implicit val executionContext: ExecutionContext) extends LazyLogging {

  val planItems = scala.collection.mutable.HashMap[String, PlanItem]()
  val planItemsHistory = scala.collection.mutable.Buffer[PlanItemHistory]()
  val caseInstanceRoles = scala.collection.mutable.HashMap[String, CaseInstanceRole]()
  val caseInstanceTeamMembers = scala.collection.mutable.HashMap[String, CaseInstanceTeamMember]() // key = <role>:<userid>
  // TODO: we need always a caseinstance (for casemodified); so no need to have it as an option?
  var caseInstance: Option[CaseInstance] = None
  var caseDefinition: CaseInstanceDefinition = null
  var caseFile: Option[ValueMap] = None

  def handleEvent(evt: CaseEvent): Future[Done] = {
    logger.debug("Handling event of type " + evt.getClass.getSimpleName + " in case " + caseInstanceId)
    evt match {
      case event: CaseDefinitionApplied => createCaseInstance(event)
      case event: PlanItemEvent => handlePlanItemEvent(event)
      case event: CaseFileEvent => handleCaseFileEvent(event)
      case event: CaseTeamEvent => handleCaseTeamEvent(event)
      case event: CaseModified => updateCaseInstance(event)
      case _ => Future.successful(Done) // Ignore other events
    }
  }

  private def createCaseInstance(event: CaseDefinitionApplied): Future[Done] = {
    this.caseInstance = Some(CaseInstanceMerger.merge(event))
    this.caseDefinition = CaseInstanceDefinition(event.getActorId, event.getCaseName, event.getDefinition.getDescription, event.getDefinition.getId, event.getDefinition.getDefinitionsDocument.getSource, event.tenant, event.createdOn, event.createdBy)
    CaseInstanceRoleMerger.merge(event).map(role => caseInstanceRoles.put(role.roleName, role))
    Future.successful(Done)
  }

  private def handlePlanItemEvent(event: PlanItemEvent): Future[Done] = {
    // Always insert new items into history, no need to first fetch them from db.
    planItemsHistory += PlanItemHistoryMerger.mapEventToHistory(event)

    event match {
      case evt: PlanItemCreated =>
        val planItem = PlanItemMerger.merge(evt)
        planItems.put(planItem.id, planItem)
        Future.successful(Done)
      case other: PlanItemEvent => {
        getPlanItem(event.getCaseInstanceId, event.getPlanItemId, event.getUser) map {
          case Some(planItem) =>
            other match {
              case evt: PlanItemTransitioned => planItems.put(planItem.id, PlanItemMerger.merge(evt, planItem))
              case evt: RepetitionRuleEvaluated => planItems.put(planItem.id, PlanItemMerger.merge(evt, planItem))
              case evt: RequiredRuleEvaluated => planItems.put(planItem.id, PlanItemMerger.merge(evt, planItem))
            }
            Done
          case None =>
            // But ... if not found, then should we create a new one here? With the PlanItemMerger that can be done ...
            logger.error("Expected PlanItem " + event.getPlanItemId + " in " + event.getCaseInstanceId + ", but not found in the database")
            Done
        }
      }
      case unknownPlanItemEvent =>
        logger.error("Apparently we have a new type of PlanItemEvent that is not being handled by this Projection. The type is " + unknownPlanItemEvent.getClass.getName)
        Future.successful(Done)
    }
  }

  private def getPlanItem(caseInstanceId: String, planItemId: String, user: TenantUser): Future[Option[PlanItem]] = {
    planItems.get(planItemId) match {
      case Some(value) =>
        logger.debug("Retrieved planitem caseinstanceid={} id={} from current transaction cache", caseInstanceId, planItemId)
        Future.successful(Some(value))
      case None =>
        logger.debug("Retrieving planitem " + planItemId + " from database")
        persistence.getPlanItem(planItemId)
    }
  }

  private def handleCaseFileEvent(event: CaseFileEvent): Future[Done] = {
    getCaseFile(event.getCaseInstanceId, event.getUser)
      .map(CaseFileMerger.merge(event, _))
      .map { data =>
        this.caseFile = Some(data)
        Done
      }
  }

  private def getCaseFile(caseInstanceId: String, user: TenantUser): Future[ValueMap] = {
    this.caseFile match {
      case Some(value) =>
        logger.debug("Retrieved casefile caseinstanceid={} from current transaction cache", caseInstanceId)
        Future.successful(value)
      case None =>
        logger.debug("Retrieving casefile caseInstanceId={} from database", caseInstanceId)
        persistence.getCaseFile(caseInstanceId).map {
          case Some(casefile) => JSONReader.parse(casefile.data)
          case None => new ValueMap()
        }
    }
  }

  private def handleCaseTeamEvent(event: CaseTeamEvent): Future[Done] = {
    event match {
      case event: TeamMemberAdded => CaseInstanceTeamMemberMerger.merge(event).foreach(member => caseInstanceTeamMembers.put(s"${member.role}:${member.userId}", member))
      case event: TeamMemberRemoved => CaseInstanceTeamMemberMerger.merge(event).foreach(member => caseInstanceTeamMembers.put(s"${member.role}:${member.userId}", member))
    }
    Future.successful(Done)
  }

  private def updateCaseInstance(event: CaseModified): Future[Done] = {
    getCaseInstance(event.getCaseInstanceId, event.getUser).map {
      case Some(value) =>
        this.caseInstance = Some(CaseInstanceMerger.merge(event, value))
        Done
      case None =>
        logger.error("Expected CaseInstance " + event.getCaseInstanceId + " but not found. CM Event:\n\n" + event.toString + "\n\n")
        Done
    }
  }

  private def getCaseInstance(caseInstanceId: String, user: TenantUser): Future[Option[CaseInstance]] = {
    caseInstance match {
      case None =>
        logger.debug("Retrieving caseinstance caseInstanceId={} from database", caseInstanceId)
        persistence.getCaseInstance(caseInstanceId)
      case Some(value) =>
        logger.debug("Retrieved caseinstance caseinstanceid={} from current transaction cache", caseInstanceId)
        Future.successful(Some(value))
    }
  }

  def commit(offsetName: String, offset: Offset, caseModified: CaseModified): Future[Done] = {
    // Gather all records inserted/updated in this transaction, and give them for bulk update

    var records = ListBuffer.empty[AnyRef]
    this.caseInstance.foreach(instance => records += instance)
    records += this.caseDefinition
    this.caseFile.foreach { caseFile =>
      records += CaseFile(caseInstanceId, tenant, caseFile.toString)
    }
    records ++= this.planItems.values.map(item => PlanItemMerger.merge(caseModified, item))
    records ++= this.planItemsHistory.map(item => PlanItemHistoryMerger.merge(caseModified, item))
    records ++= this.caseInstanceRoles.values
    records ++= this.caseInstanceTeamMembers.values

    // If we reach this point, we have real events handled and content added,
    // so also update the offset of the last event handled in this projection
    records += NamedOffset(offsetName, offset)

    //    println("Committing "+records.size+" records into the database for "+offset)

    persistence.bulkUpdate(records.filter(r => r != null))
  }
}
