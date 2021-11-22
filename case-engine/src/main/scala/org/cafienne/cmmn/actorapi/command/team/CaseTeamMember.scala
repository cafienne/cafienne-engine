package org.cafienne.cmmn.actorapi.command.team

import org.cafienne.actormodel.identity.UserIdentity
import org.cafienne.cmmn.definition.CaseDefinition
import org.cafienne.cmmn.instance.team.CaseTeamError
import org.cafienne.infrastructure.serialization.Fields
import org.cafienne.json._

import scala.jdk.CollectionConverters._

case class CaseTeamMember(key: MemberKey, caseRoles: Seq[String] = Seq(), isOwner: Option[Boolean] = None, removeRoles: Seq[String] = Seq()) extends CafienneJson {
  def validateRolesExist(caseDefinition: CaseDefinition): Unit = {
    val blankRoles = (caseRoles ++ removeRoles).filter(roleName => roleName.isBlank)
    val undefinedRoles = (caseRoles ++ removeRoles).filter(roleName => caseDefinition.getCaseTeamModel().getCaseRole(roleName) == null)

    if (blankRoles.nonEmpty || undefinedRoles.nonEmpty) {
      if (undefinedRoles.isEmpty) {
        throw new CaseTeamError("An empty role is not permitted")
      } else {
        throw new CaseTeamError("The following role(s) are not defined in the case: " + undefinedRoles.mkString(","))
      }
    }
  }

  def isTenantUser(): Boolean = {
    key.`type`.equals("user")
  }

  def getCaseRoles = {
    caseRoles.asJava
  }

  def rolesToRemove = {
    removeRoles.asJava
  }

  override def toValue: Value[_] = {
    val json = new ValueMap("memberId", key.id,
      "memberType", key.`type`,
      "isOwner", isOwner.fold(Value.NULL.asInstanceOf[Value[Any]])(b => new BooleanValue(b).asInstanceOf[Value[Any]]),
      "caseRoles", Value.convert(caseRoles))

    // Only serialize remove roles if there are any. This check is required, because reading case team should not return that field
    if (removeRoles.nonEmpty) json.put("removeRoles", Value.convert(removeRoles))
    json
  }
}

object CaseTeamMember {

  def apply(key: MemberKey, caseRoles: Array[String], isOwner: Boolean) = new CaseTeamMember(key, caseRoles = caseRoles.toSeq, isOwner = Some(isOwner))

  def apply(key: MemberKey, caseRoles: Set[String], isOwner: Boolean) = new CaseTeamMember(key, caseRoles = caseRoles.toSeq, isOwner = Some(isOwner))

  def createBootstrapMember(user: UserIdentity) = new CaseTeamMember(MemberKey(user.id, "user"), isOwner = Some(true))

  def deserialize(json: ValueMap) = {
    val memberId = json.readString(Fields.memberId)
    val memberType = json.readString(Fields.memberType)
    val isOwner = {
      val v = json.get("isOwner")
      if (v.equals(Value.NULL)) {
        None
      } else {
        Some(v.getValue.asInstanceOf[Boolean])
      }
    }

    val caseRoles = json.readStringList(Fields.caseRoles).toSeq
    val removeRoles = json.readStringList(Fields.removeRoles).toSeq
    new CaseTeamMember(MemberKey(memberId, memberType), caseRoles = caseRoles, isOwner = isOwner, removeRoles = removeRoles)
  }
}

