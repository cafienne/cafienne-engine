package org.cafienne.board.state.definition

import org.cafienne.board.state.StateElement

trait DefinitionElement extends StateElement {
  val definition: BoardDefinition = state.definition
}
