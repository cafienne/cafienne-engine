package org.cafienne.service.api.projection.task

import org.cafienne.cmmn.akka.event.CaseModified
import org.cafienne.humantask.akka.event._
import org.cafienne.service.api.tasks.Task

object TaskMerger {
  // TODO: will be good if we add more fields to Task, like last action?
  def apply(evt: HumanTaskTransitioned, current: Task): Task = current.copy(taskState = evt.getCurrentState.name)
  def apply(evt: HumanTaskActivated, current: Task): Task = current.copy(role = evt.getPerformer, taskModel = evt.getTaskModel.toString)
  def apply(evt: HumanTaskAssigned, current: Task): Task = current.copy(assignee = evt.assignee)
  def apply(evt: HumanTaskRevoked, current: Task): Task = current.copy(assignee = evt.assignee)
  def apply(evt: HumanTaskCompleted, current: Task): Task = current.copy(output = evt.getTaskOutput.toString)
  def apply(evt: HumanTaskDueDateFilled, current: Task): Task = current.copy(dueDate = Some(evt.dueDate))
  def apply(evt: HumanTaskOwnerChanged, current: Task): Task = current.copy(owner = evt.owner)
  def apply(evt: HumanTaskOutputSaved, current: Task): Task = current.copy(output = evt.getTaskOutput.toString)
  def apply(evt: HumanTaskInputSaved, current: Task): Task = current.copy(input = evt.getInput.toString)
  def apply(evt: CaseModified, current: Task): Task = current.copy(modifiedBy = evt.getUser.id, lastModified = evt.lastModified)
}
