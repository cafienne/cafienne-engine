package org.cafienne.cmmn.actorapi.command.team

case class MemberKey(id: String, `type`: String) {
  override def toString: String = s"${`type`} '$id'"

  def same(that: MemberKey): Boolean = {
    this.id.equals(that.id) && this.`type`.equals(that.`type`)
  }
}
