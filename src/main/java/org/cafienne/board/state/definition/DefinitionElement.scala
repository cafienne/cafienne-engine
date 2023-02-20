package org.cafienne.board.state.definition

import org.cafienne.board.BoardActor
import org.cafienne.board.state.StateElement

trait DefinitionElement extends StateElement {
  val definition: BoardDefinition
  val board: BoardActor = definition.board
}
