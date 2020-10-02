package org.cafienne.service.api.writer

import java.time.Instant

import org.cafienne.akka.actor.CaseSystem
import org.cafienne.akka.actor.identity.TenantUser
import org.cafienne.akka.actor.serialization.Fields
import org.cafienne.akka.actor.serialization.json.ValueMap
import org.cafienne.cmmn.akka.event._
import org.cafienne.cmmn.akka.event.file.{CaseFileEvent, CaseFileItemCreated}
import org.cafienne.cmmn.akka.event.plan.{PlanItemCreated, PlanItemTransitioned}
import org.cafienne.cmmn.definition.CaseDefinition
import org.cafienne.cmmn.instance.casefile.CaseFileItemTransition
import org.cafienne.cmmn.instance.{State, Transition}

/**
  * Intended to create events for projection testcases
  * @param actorId
  * @param caseDefinition
  * @param user
  */
class EventFactory(actorId: String, caseDefinition: CaseDefinition, user: TenantUser) {

  def createCaseDefinitionApplied(user: TenantUser = user) : CaseDefinitionApplied = {
    val json = new ValueMap(
      Fields.caseName, caseDefinition.getName
      ,Fields.definition, caseDefinition.toJSON
      ,Fields.engineVersion, CaseSystem.version.json
      ,Fields.parentActorId, ""
      ,Fields.rootActorId, actorId
      ,Fields.createdOn, Instant.now
      ,Fields.createdBy, user.id
      ,Fields.modelEvent, getModelEvent(user)
    )
    new CaseDefinitionApplied(json)
  }

  def createCaseModified(lastModified: Instant, user: TenantUser = user) : CaseModified = {
    val json = new ValueMap(
      Fields.lastModified, lastModified.toString
      ,Fields.numFailures, Integer.valueOf(0)
      ,Fields.state, State.Active.toString
      ,Fields.modelEvent, getModelEvent(user)
    )
    new CaseModified(json)
  }

  def createPlanItemCreated(planItemId: String, planItemType: String, name: String, stageId: String, lastModified: Instant, user: TenantUser = user): PlanItemCreated = {
    val json = new ValueMap(
      Fields.name, name
      ,Fields.createdOn, Instant.now
      ,Fields.createdBy, user.id
      ,Fields.stageId, stageId
      ,Fields.planItemId, planItemId
      ,Fields.`type`, planItemType
      ,Fields.planitem, new ValueMap(
        Fields.seqNo, Integer.valueOf(1)
        ,Fields.index, Integer.valueOf(0)
      )
      ,Fields.modelEvent, getModelEvent(user)
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
      Fields.currentState, currentState.toString
      ,Fields.historyState, historyState.toString
      ,Fields.transition, transition.toString
      ,Fields.planItemId, planItemId
      ,Fields.`type`, planItemType
      ,Fields.planitem, new ValueMap(
        Fields.seqNo, Integer.valueOf(1)
        ,Fields.index, Integer.valueOf(0)
      )
      ,Fields.modelEvent, getModelEvent(user)
    )
    new PlanItemTransitioned(json)
  }


  def createCaseFileEvent(path: String, value: ValueMap, transition: CaseFileItemTransition, index: Int = -1, user: TenantUser = user): CaseFileEvent = {
    val json = new ValueMap(
      Fields.path, path
      ,Fields.value, value
      ,Fields.transition, transition.toString
      ,Fields.index, Integer.valueOf(index)
//      ,Fields.name, name
//      ,Fields.moment, moment.toString
//      ,Fields.state, state
      ,Fields.modelEvent, getModelEvent(user)
    )
    new CaseFileItemCreated(json)
  }

  private def getModelEvent(user: TenantUser) : ValueMap = {
    new ValueMap(
      Fields.actorId, actorId
      ,Fields.user, user
    )
  }
}
