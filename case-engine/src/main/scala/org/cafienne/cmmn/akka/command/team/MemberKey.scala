package org.cafienne.cmmn.akka.command.team

case class MemberKey(id: String, `type`: String) {
  override def toString: String = s"${`type`} '$id'"
}
