package org.cafienne.board.actorapi.command.definition

import org.cafienne.board.actorapi.event.definition.BoardDefinitionEvent
import org.cafienne.board.state.definition.FormElement
import org.cafienne.json.ValueMap

trait UpdateFormElement {
  val title: Option[String]
  val form: Option[ValueMap]
  val role: Option[String] = None

  protected def hasChanges(element: FormElement): Boolean = {
    titleChanged(element.getTitle) || formChanged(element.getForm) || roleChanged(element.getRole)
  }

  /**
    * Checks if any of the command fields need to be updated in the definition element,
    * and, if so, instantiates and adds the relevant update event.
    */
  protected def runChangeDetector[E <: BoardDefinitionEvent](element: FormElement, eventCreator: (String, ValueMap, String) => E): Unit = {
    if (hasChanges(element)) {
      val newTitle = title.getOrElse(element.getTitle)
      val newForm = form.getOrElse(element.getForm)
      val newRole = role.getOrElse(element.getRole)
      val event = eventCreator(newTitle, newForm, newRole)
      element.addEvent(event)
    }
  }

  protected def titleChanged(currentTitle: String): Boolean = title.fold(false)(currentTitle != _)

  protected def roleChanged(currentRole: String): Boolean = role.fold(false)(currentRole != _)

  protected def formChanged(currentForm: ValueMap): Boolean = form.fold(false)(currentForm != _)
}
