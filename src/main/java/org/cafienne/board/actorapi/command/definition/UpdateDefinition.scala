package org.cafienne.board.actorapi.command.definition

import org.cafienne.json.ValueMap

trait UpdateDefinition {
  val title: Option[String]
  val role: Option[String]
  val form: Option[ValueMap]

  protected def titleChanged(currentTitle: String): Boolean = title.fold(false)(currentTitle != _)

  protected def roleChanged(currentRole: String): Boolean = role.fold(false)(currentRole != _)

  protected def formChanged(currentForm: ValueMap) = form.fold(false)(currentForm != _)
}
