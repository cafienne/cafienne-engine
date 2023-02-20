package org.cafienne.board.state.definition

import org.cafienne.board.state.StateElement
import org.cafienne.json.ValueMap

trait FormElement extends StateElement {
  protected var title: String = ""
  protected var role: String = ""
  protected var form: ValueMap = new ValueMap()

  def getTitle: String = title

  def getRole: String = role

  def getForm: ValueMap = form
}
