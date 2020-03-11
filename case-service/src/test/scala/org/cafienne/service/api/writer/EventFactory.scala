package org.cafienne.service.api.writer

import java.time.Instant

import org.cafienne.akka.actor.CaseSystem
import org.cafienne.akka.actor.event.ModelEvent
import org.cafienne.akka.actor.identity.TenantUser
import org.cafienne.cmmn.akka.event._
import org.cafienne.cmmn.akka.event.plan.{PlanItemCreated, PlanItemEvent, PlanItemTransitioned}
import org.cafienne.cmmn.definition.CaseDefinition
import org.cafienne.cmmn.instance.casefile.ValueMap
import org.cafienne.cmmn.instance.{CaseFileItemTransition, State, Transition}

/**
  * Intended to create events for projection testcases
  * @param actorId
  * @param caseDefinition
  * @param user
  */
class EventFactory(actorId: String, caseDefinition: CaseDefinition, user: TenantUser) {

  def createCaseDefinitionApplied(user: TenantUser = user) : CaseDefinitionApplied = {
    val json = new ValueMap(
      CaseDefinitionApplied.Fields.caseName, caseDefinition.getName
      ,CaseDefinitionApplied.Fields.definition, caseDefinition.toJSON
      ,CaseDefinitionApplied.Fields.engineVersion, CaseSystem.version.json
      ,CaseDefinitionApplied.Fields.parentActorId, ""
      ,CaseDefinitionApplied.Fields.rootActorId, actorId
      ,CaseDefinitionApplied.Fields.createdOn, Instant.now
      ,CaseDefinitionApplied.Fields.createdBy, user.id
      ,ModelEvent.Fields.modelEvent, getModelEvent(user)
    )
    new CaseDefinitionApplied(json)
  }

  def createCaseModified(lastModified: Instant, user: TenantUser = user) : CaseModified = {
    val json = new ValueMap(
      CaseModified.Fields.lastModified, lastModified.toString
      ,CaseModified.Fields.numFailures, Integer.valueOf(0)
      ,CaseModified.Fields.state, State.Active.toString
      ,ModelEvent.Fields.modelEvent, getModelEvent(user)
    )
    new CaseModified(json)
  }

  def createPlanItemCreated(planItemId: String, planItemType: String, name: String, stageId: String, lastModified: Instant, user: TenantUser = user): PlanItemCreated = {
    val json = new ValueMap(
      PlanItemCreated.Fields.name, name
      ,PlanItemCreated.Fields.createdOn, Instant.now
      ,PlanItemCreated.Fields.createdBy, user.id
      ,PlanItemCreated.Fields.stageId, stageId
      ,PlanItemEvent.Fields.planItemId, planItemId
      ,PlanItemEvent.Fields.`type`, planItemType
      ,PlanItemEvent.Fields.planitem, new ValueMap(
        PlanItemEvent.Fields.seqNo, Integer.valueOf(1)
        ,PlanItemEvent.Fields.index, Integer.valueOf(0)
      )
      ,ModelEvent.Fields.modelEvent, getModelEvent(user)
    )
    new PlanItemCreated(json)
  }

  def createPlanItemTransitioned(planItemId: String,
                                 planItemType: String,
                                 currentState: State,
                                 historyState: State,
                                 transition: Transition,
                                 lastModified: Instant,
                                 user: TenantUser = user): PlanItemTransitioned = {
    val json = new ValueMap(
      PlanItemTransitioned.Fields.currentState, currentState.toString
      ,PlanItemTransitioned.Fields.historyState, historyState.toString
      ,PlanItemTransitioned.Fields.transition, transition.toString
      ,PlanItemEvent.Fields.planItemId, planItemId
      ,PlanItemEvent.Fields.`type`, planItemType
      ,PlanItemEvent.Fields.planitem, new ValueMap(
        PlanItemEvent.Fields.seqNo, Integer.valueOf(1)
        ,PlanItemEvent.Fields.index, Integer.valueOf(0)
      )
      ,ModelEvent.Fields.modelEvent, getModelEvent(user)
    )
    new PlanItemTransitioned(json)
  }


  def createCaseFileEvent(path: String, value: ValueMap, transition: CaseFileItemTransition, index: Int = -1, user: TenantUser = user): CaseFileEvent = {
    val json = new ValueMap(
      CaseFileEvent.Fields.path, path
      ,CaseFileEvent.Fields.value, value
      ,CaseFileEvent.Fields.transition, transition.toString
      ,CaseFileEvent.Fields.index, Integer.valueOf(index)
//      ,CaseFileEvent.Fields.name, name
//      ,CaseFileEvent.Fields.moment, moment.toString
//      ,CaseFileEvent.Fields.state, state
      ,ModelEvent.Fields.modelEvent, getModelEvent(user)
    )
    new CaseFileEvent(json)
  }

  private def getModelEvent(user: TenantUser) : ValueMap = {
    new ValueMap(
      ModelEvent.Fields.actorId, actorId
      ,ModelEvent.Fields.user, user
    )
  }
}
