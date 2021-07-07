package org.cafienne.service.db.materializer.cases

import akka.Done
import akka.persistence.query.Offset
import com.typesafe.scalalogging.LazyLogging
import org.cafienne.actormodel.event.TransactionEvent
import org.cafienne.actormodel.identity.TenantUser
import org.cafienne.cmmn.actorapi.event._
import org.cafienne.cmmn.actorapi.event.file.{BusinessIdentifierCleared, BusinessIdentifierEvent, BusinessIdentifierSet, CaseFileEvent}
import org.cafienne.cmmn.actorapi.event.plan._
import org.cafienne.cmmn.actorapi.event.team._
import org.cafienne.humantask.actorapi.event._
import org.cafienne.infrastructure.cqrs.OffsetRecord
import org.cafienne.json.{JSONReader, ValueMap}
import org.cafienne.service.db.materializer.RecordsPersistence
import org.cafienne.service.db.materializer.slick.SlickTransaction
import org.cafienne.service.db.record._

import scala.collection.mutable.ListBuffer
import scala.concurrent.{ExecutionContext, Future}

class CaseTransaction(caseInstanceId: String, tenant: String, persistence: RecordsPersistence)(implicit val executionContext: ExecutionContext) extends SlickTransaction[CaseEvent] with LazyLogging {

  val planItems = scala.collection.mutable.HashMap[String, PlanItemRecord]()
  val planItemsHistory = scala.collection.mutable.Buffer[PlanItemHistoryRecord]()
  val caseInstanceRoles = scala.collection.mutable.HashMap[String, CaseRoleRecord]()
  val caseInstanceTeamMembers = scala.collection.mutable.HashMap[(String, String, Boolean, String), CaseTeamMemberRecord]()
  val businessIdentifiers = scala.collection.mutable.Set[CaseBusinessIdentifierRecord]()
  val tasks = scala.collection.mutable.HashMap[String, TaskRecord]()
  // TODO: we need always a caseinstance (for casemodified); so no need to have it as an option?
  var caseInstance: Option[CaseRecord] = None
  var caseDefinition: CaseDefinitionRecord = null
  var caseFile: Option[ValueMap] = None

  override def handleEvent(evt: CaseEvent, offsetName: String, offset: Offset): Future[Done] = {
    logger.debug("Handling event of type " + evt.getClass.getSimpleName + " in case " + caseInstanceId)
    evt match {
      case event: CaseDefinitionApplied => createCaseInstance(event)
      case event: PlanItemEvent => handlePlanItemEvent(event)
      case event: CaseFileEvent => handleCaseFileEvent(event)
      case event: CaseTeamEvent => handleCaseTeamEvent(event)
      case event: HumanTaskCreated => deprecatedCreateTask(event)
      case event: HumanTaskActivated => createTask(event)
      case event: HumanTaskEvent => handleHumanTaskEvent(event)
      case event: CaseAppliedPlatformUpdate => updateUserIds(event, offsetName, offset)
      case event: CaseModified => updateCaseInstance(event)
      case event: BusinessIdentifierEvent => handleBusinessIdentifierEvent(event)
      case _ => Future.successful(Done) // Ignore other events
    }
  }

  private def createCaseInstance(event: CaseDefinitionApplied): Future[Done] = {
    this.caseInstance = Some(CaseInstanceMerger.merge(event))
    this.caseDefinition = CaseDefinitionRecord(event.getActorId, event.getCaseName, event.getDefinition.documentation.text, event.getDefinition.getId, event.getDefinition.getDefinitionsDocument.getSource, event.tenant, event.createdOn, event.createdBy)
    this.caseFile = Some(new ValueMap()) // Always create an empty case file
    CaseInstanceRoleMerger.merge(event).map(role => caseInstanceRoles.put(role.roleName, role))
    Future.successful(Done)
  }

  private def handlePlanItemEvent(event: PlanItemEvent): Future[Done] = {
    // Always insert new items into history, no need to first fetch them from db.
    PlanItemHistoryMerger.mapEventToHistory(event).foreach(item => planItemsHistory += item)

    event match {
      case evt: PlanItemCreated =>
        val planItem = PlanItemMerger.merge(evt)
        planItems.put(planItem.id, planItem)
        Future.successful(Done)
      case other: PlanItemEvent => {
        getPlanItem(event.getPlanItemId) map {
          case Some(planItem) =>
            other match {
              case evt: PlanItemTransitioned => planItems.put(planItem.id, PlanItemMerger.merge(evt, planItem))
              case evt: RepetitionRuleEvaluated => planItems.put(planItem.id, PlanItemMerger.merge(evt, planItem))
              case evt: RequiredRuleEvaluated => planItems.put(planItem.id, PlanItemMerger.merge(evt, planItem))
              case _ => // Nothing to do for the other events
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

  private def getPlanItem(planItemId: String) = {
    planItems.get(planItemId) match {
      case Some(value) =>
        logger.debug(s"Found plan item $planItemId in current transaction cache")
        Future.successful(Some(value))
      case None =>
        logger.debug(s"Retrieving plan item $planItemId from database")
        persistence.getPlanItem(planItemId)
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

  private val bufferedCaseFileEvents = new CaseFileEventBuffer()

  private def handleCaseFileEvent(event: CaseFileEvent): Future[Done] = {
    bufferedCaseFileEvents.addEvent(event)
    getCaseFile(event.getCaseInstanceId, event.getUser)
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
    // We handle 2 types of event: either the old ones (which carried all info in one shot) or the new ones, which are more particular
    event match {
      case event: DeprecatedCaseTeamEvent => {
        // Deprecated case team events have all member roles in them; these members are always of type user; all those users become owner and active;
        import scala.jdk.CollectionConverters._
        // We need to add the empty role (if not yet there),
        //  in order to have member table also populated when a member has no roles but still is part of the team
        val roles = event.getRoles().asScala ++ Seq("")
        // Now determine whether the user (and it's roles) become active (and then also owner) or de-activated
        val enabled = if (event.isInstanceOf[TeamMemberAdded]) true else false // Both for ownership and active
        // For reach role add a record.
        roles.map(role => {
          val key = (event.getActorId, event.getUserId, true, role)
          val record = CaseTeamMemberRecord(event.getActorId, tenant = tenant, memberId = event.getUserId, caseRole = role, isTenantUser = true, isOwner = enabled, active = enabled)
          caseInstanceTeamMembers.put(key, record)
        })
      }
      // New type of event:
      case event: CaseTeamMemberEvent => {
        val key = (event.getActorId, event.memberId, event.isTenantUser, event.roleName)
        // Make sure to update any existing versions of the record (especially if first a user is added and at the same time becomes owner this is necessary)
        //  We have seen situation with SQL Server where the order of the update actually did not make a user owner
        val member = caseInstanceTeamMembers.getOrElseUpdate(key, CaseTeamMemberRecord(event.getActorId, tenant = tenant, caseRole = event.roleName, isTenantUser = event.isTenantUser, memberId = event.memberId, isOwner = false, active = true))
        event match {
          case _: TeamRoleFilled => caseInstanceTeamMembers.put(key, member.copy(active = true))
          case _: TeamRoleCleared => caseInstanceTeamMembers.put(key, member.copy(active = false))
          case _: CaseOwnerAdded => caseInstanceTeamMembers.put(key, member.copy(isOwner = true))
          case _: CaseOwnerRemoved => caseInstanceTeamMembers.put(key, member.copy(isOwner = false))
          case _ => // Ignore other events
        }
      }
      case _ => // Ignore other events
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

  private def getCaseInstance(caseInstanceId: String, user: TenantUser): Future[Option[CaseRecord]] = {
    caseInstance match {
      case None =>
        logger.debug("Retrieving caseinstance caseInstanceId={} from database", caseInstanceId)
        persistence.getCaseInstance(caseInstanceId)
      case Some(value) =>
        logger.debug("Retrieved caseinstance caseinstanceid={} from current transaction cache", caseInstanceId)
        Future.successful(Some(value))
    }
  }

  def createTask(evt: HumanTaskActivated): Future[Done] = {
    // See above comments. HumanTaskActivated has replaced HumanTaskCreated.
    //  We check here to see if our version is an old or a new one, by checking whether
    //  a task is already available in the transaction (that means HumanTaskCreated was still there, the old format).
    val updatedTask = this.tasks.get(evt.taskId) match {
      case None => {
        // New format. We will create task here
        TaskRecord(id = evt.taskId,
          caseInstanceId = evt.getActorId,
          tenant = evt.tenant,
          taskName = evt.getTaskName,
          createdOn = evt.getCreatedOn,
          createdBy = evt.getCreatedBy,
          lastModified = evt.getCreatedOn,
          modifiedBy = evt.getCreatedBy,
          role = evt.getPerformer,
          taskState = evt.getCurrentState.name,
          taskModel = evt.getTaskModel.toString)
      }
      case Some(task) => {
        // Old format, must have been created in same transaction through HumanTaskCreated, fine too
        task.copy(role = evt.getPerformer, taskModel = evt.getTaskModel.toString, taskState = evt.getCurrentState.name)
      }
    }
    this.tasks.put(evt.taskId, updatedTask)
    Future.successful{ Done }
  }

  def deprecatedCreateTask(evt: HumanTaskCreated): Future[Done] = {
    this.tasks.put(evt.taskId, TaskRecord(id = evt.taskId,
      caseInstanceId = evt.getActorId,
      tenant = evt.tenant,
      taskName = evt.getTaskName,
      createdOn = evt.getCreatedOn,
      createdBy = evt.getCreatedBy,
      lastModified = evt.getCreatedOn,
      modifiedBy = evt.getCreatedBy,
    ))
    Future.successful{ Done }
  }

  def handleHumanTaskEvent(event: HumanTaskEvent) = {
    val fTask: Future[Option[TaskRecord]] = {
      event match {
        case evt: HumanTaskInputSaved => fetchTask(event.taskId).map(t => t.map(task => TaskMerger(evt, task)))
        case evt: HumanTaskOutputSaved => fetchTask(event.taskId).map(t => t.map(task => TaskMerger(evt, task)))
        case evt: HumanTaskOwnerChanged => fetchTask(event.taskId).map(t => t.map(task => TaskMerger(evt, task)))
        case evt: HumanTaskDueDateFilled => fetchTask(event.taskId).map(t => t.map(task => TaskMerger(evt, task)))
        case evt: HumanTaskTransitioned => fetchTask(event.taskId).map(task => task.map(t => {
          val copy = TaskMerger(evt, t)
          evt match {
            case evt: HumanTaskAssigned => TaskMerger(evt, copy)
            case evt: HumanTaskActivated => TaskMerger(evt, copy)
            case evt: HumanTaskCompleted => TaskMerger(evt, copy)
            case evt: HumanTaskTerminated => TaskMerger(evt, copy)
            case other => {
              System.err.println("We missed out on HumanTaskTransition event of type " + other.getClass.getName)
              copy
            }
          }
        }))
        case _ => Future.successful(None) // Ignore and error on other events
      }
    }

    fTask.map {
      case Some(task) => this.tasks.put(task.id, task)
      case _ => logger.error("Could not find task with id " + event.taskId + " in the current database. This may lead to problems. Ignoring event of type " + event.getClass.getName)
    }.flatMap(_ => Future.successful(Done))

  }

  def updateUserIds(event: CaseAppliedPlatformUpdate, offsetName: String, offset: Offset): Future[Done] = {
    persistence.updateCaseUserInformation(event.getCaseInstanceId, event.newUserInformation.info, offsetName, offset)
  }

  private def fetchTask(taskId: String) = {
    this.tasks.get(taskId) match {
      case None =>
        logger.debug("Retrieving task " + taskId + " from database")
        persistence.getTask(taskId)
      case Some(task) => Future.successful(Some(task))
    }
  }

  /**
    * Depending on the presence of CaseFileEvents this will add a new CaseFileRecord
    * @param caseFileInProgress
    * @return
    */
  private def getNewCaseFile(caseFileInProgress: ValueMap): CaseFileRecord = {
    bufferedCaseFileEvents.events.forEach(event => CaseFileMerger.merge(event, caseFileInProgress))
    CaseFileRecord(caseInstanceId, tenant, caseFileInProgress.toString)
  }

  override def commit(offsetName: String, offset: Offset, caseModified: TransactionEvent[_]): Future[Done] = {
    // Gather all records inserted/updated in this transaction, and give them for bulk update

    val records = ListBuffer.empty[AnyRef]
    this.caseInstance.foreach(instance => records += instance)
    records += this.caseDefinition
    this.caseFile.foreach { caseFile =>
      records += getNewCaseFile(caseFile)
    }
    records ++= this.planItems.values.map(item => PlanItemMerger.merge(caseModified, item))
    records ++= this.planItemsHistory.map(item => PlanItemHistoryMerger.merge(caseModified, item))
    records ++= this.caseInstanceRoles.values
    records ++= this.caseInstanceTeamMembers.values
    records ++= this.businessIdentifiers.toSeq
    records ++= this.tasks.values.map(current => TaskMerger(caseModified, current))

    // If we reach this point, we have real events handled and content added,
    // so also update the offset of the last event handled in this projection
    records += OffsetRecord(offsetName, offset)

    //    println("Committing "+records.size+" records into the database for "+offset)

    persistence.bulkUpdate(records.toSeq.filter(r => r != null))
  }
}
