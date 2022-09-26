package org.cafienne.querydb.materializer.cases.plan

import org.cafienne.cmmn.actorapi.event.CaseModified
import org.cafienne.humantask.actorapi.event._
import org.cafienne.humantask.actorapi.event.migration.HumanTaskMigrated
import org.cafienne.querydb.record.TaskRecord

object TaskMerger {
  def create(evt: HumanTaskActivated): TaskRecord = TaskRecord(id = evt.getTaskId, caseInstanceId = evt.getActorId, tenant = evt.tenant, taskName = evt.getTaskName, createdOn = evt.getCreatedOn, createdBy = evt.getCreatedBy, lastModified = evt.getCreatedOn, modifiedBy = evt.getCreatedBy, role = evt.getPerformer, taskState = evt.getCurrentState.name, taskModel = evt.getTaskModel)

  def create(evt: HumanTaskCreated): TaskRecord = TaskRecord(id = evt.getTaskId, caseInstanceId = evt.getActorId, tenant = evt.tenant, taskName = evt.getTaskName, createdOn = evt.getCreatedOn, createdBy = evt.getCreatedBy, lastModified = evt.getCreatedOn, modifiedBy = evt.getCreatedBy)

  def apply(evt: HumanTaskActivated, current: TaskRecord): TaskRecord = current.copy(role = evt.getPerformer, taskModel = evt.getTaskModel, taskState = evt.getCurrentState.name)

  def apply(evt: HumanTaskTransitioned, current: TaskRecord): TaskRecord = current.copy(taskState = evt.getCurrentState.name)

  def apply(evt: HumanTaskAssigned, current: TaskRecord): TaskRecord = current.copy(assignee = evt.assignee)

  def apply(evt: HumanTaskRevoked, current: TaskRecord): TaskRecord = current.copy(assignee = evt.assignee)

  def apply(evt: HumanTaskCompleted, current: TaskRecord): TaskRecord = current.copy(output = evt.getTaskOutput.toString)

  def apply(evt: HumanTaskTerminated, current: TaskRecord): TaskRecord = current // Nothing to do

  def apply(evt: HumanTaskDueDateFilled, current: TaskRecord): TaskRecord = current.copy(dueDate = Some(evt.dueDate))

  def apply(evt: HumanTaskOwnerChanged, current: TaskRecord): TaskRecord = current.copy(owner = evt.owner)

  def apply(evt: HumanTaskOutputSaved, current: TaskRecord): TaskRecord = current.copy(output = evt.getTaskOutput.toString)

  def apply(evt: HumanTaskInputSaved, current: TaskRecord): TaskRecord = current.copy(input = evt.getInput.toString)

  def apply(evt: HumanTaskMigrated, current: TaskRecord): TaskRecord = current.copy(modifiedBy = evt.getUser.id, lastModified = evt.getTimestamp, taskModel = evt.getTaskModel, taskName = evt.getTaskName, role = evt.getPerformer)

  def apply(evt: CaseModified, current: TaskRecord): TaskRecord = current.copy(modifiedBy = evt.getUser.id, lastModified = evt.lastModified)
}
