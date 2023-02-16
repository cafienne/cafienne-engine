package org.cafienne.board.actorapi.command.definition

import org.cafienne.json.ValueMap

trait UpdateFormElement {
  val title: Option[String]
  val form: Option[ValueMap]
  val role: Option[String] = None

  protected def titleChanged(currentTitle: String): Boolean = title.fold(false)(currentTitle != _)

  protected def roleChanged(currentRole: String): Boolean = role.fold(false)(currentRole != _)

  protected def formChanged(currentForm: ValueMap): Boolean = form.fold(false)(currentForm != _)
}
