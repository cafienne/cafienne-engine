package org.cafienne.service.api.projection.cases

import org.cafienne.akka.actor.event.TransactionEvent
import org.cafienne.humantask.actorapi.event._
import org.cafienne.service.api.projection.record.TaskRecord

object TaskMerger {
  // TODO: will be good if we add more fields to Task, like last action?
  def apply(evt: HumanTaskTransitioned, current: TaskRecord): TaskRecord = current.copy(taskState = evt.getCurrentState.name)
  def apply(evt: HumanTaskAssigned, current: TaskRecord): TaskRecord = current.copy(assignee = evt.assignee)
  def apply(evt: HumanTaskRevoked, current: TaskRecord): TaskRecord = current.copy(assignee = evt.assignee)
  def apply(evt: HumanTaskCompleted, current: TaskRecord): TaskRecord = current.copy(output = evt.getTaskOutput.toString)
  def apply(evt: HumanTaskTerminated, current: TaskRecord): TaskRecord = current // Nothing to do
  def apply(evt: HumanTaskDueDateFilled, current: TaskRecord): TaskRecord = current.copy(dueDate = Some(evt.dueDate))
  def apply(evt: HumanTaskOwnerChanged, current: TaskRecord): TaskRecord = current.copy(owner = evt.owner)
  def apply(evt: HumanTaskOutputSaved, current: TaskRecord): TaskRecord = current.copy(output = evt.getTaskOutput.toString)
  def apply(evt: HumanTaskInputSaved, current: TaskRecord): TaskRecord = current.copy(input = evt.getInput.toString)
  def apply(evt: TransactionEvent[_], current: TaskRecord): TaskRecord = current.copy(modifiedBy = evt.getUser.id, lastModified = evt.lastModified)

  def apply(evt: HumanTaskActivated, current: TaskRecord): TaskRecord = {
    // We should never reach this point
    System.err.println("Touching deprecated code. Should not be reachable at all ...")
    current.copy(role = evt.getPerformer, taskModel = evt.getTaskModel.toString)
  }
}
