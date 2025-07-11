package org.cafienne.persistence.querydb.materializer

import org.cafienne.actormodel.identity.TenantUser
import org.cafienne.engine.cmmn.actorapi.event._
import org.cafienne.engine.cmmn.actorapi.event.file.{CaseFileItemCreated, CaseFileItemTransitioned}
import org.cafienne.engine.cmmn.actorapi.event.plan.{PlanItemCreated, PlanItemTransitioned}
import org.cafienne.engine.cmmn.definition.CaseDefinition
import org.cafienne.engine.cmmn.instance.casefile.CaseFileItemTransition
import org.cafienne.engine.cmmn.instance.{PlanItemType, State, Transition}
import org.cafienne.infrastructure.serialization.Fields
import org.cafienne.json.ValueMap
import org.cafienne.system.CaseSystem

import java.time.Instant

/**
  * Intended to create events for projection testcases
  * @param actorId
  * @param caseDefinition
  * @param user
  */
class EventFactory(caseSystem: CaseSystem, actorId: String, caseDefinition: CaseDefinition, user: TenantUser) {

  def createCaseDefinitionApplied(user: TenantUser = user) : CaseDefinitionApplied = {
    val json = new ValueMap(
      Fields.caseName, caseDefinition.getName
      ,Fields.definition, caseDefinition.toJSON
      ,Fields.engineVersion, caseSystem.version.json
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

  def createPlanItemCreated(planItemId: String, planItemType: PlanItemType, name: String, stageId: String, user: TenantUser = user): PlanItemCreated = {
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

  def createPlanItemTransitioned(planItemId: String, planItemType: PlanItemType, currentState: State, historyState: State, transition: Transition, user: TenantUser = user): PlanItemTransitioned = {
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


  def createCaseFileEvent(path: String, value: ValueMap, transition: CaseFileItemTransition, index: Int = -1, user: TenantUser = user): CaseFileItemTransitioned = {
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
