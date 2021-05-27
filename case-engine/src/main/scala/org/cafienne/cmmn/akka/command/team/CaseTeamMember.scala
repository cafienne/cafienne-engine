package org.cafienne.cmmn.akka.command.team

import org.cafienne.akka.actor.identity.TenantUser
import org.cafienne.akka.actor.serialization.json.{BooleanValue, StringValue, Value, ValueMap}
import org.cafienne.cmmn.definition.CaseDefinition
import org.cafienne.cmmn.instance.team.CaseTeamError
import org.cafienne.infrastructure.json.CafienneJson

case class CaseTeamMember(key: MemberKey, caseRoles: Seq[String] = Seq(), isOwner: Option[Boolean] = None, removeRoles: Seq[String] = Seq()) extends CafienneJson {
  def validateRolesExist(caseDefinition: CaseDefinition): Unit = {
    val blankRoles = (caseRoles ++ removeRoles).filter(roleName => roleName.isBlank)
    val undefinedRoles = (caseRoles ++ removeRoles).filter(roleName => caseDefinition.getCaseRole(roleName) == null)

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
    import scala.collection.JavaConverters._
    caseRoles.asJava
  }

  def rolesToRemove = {
    import scala.collection.JavaConverters._
    removeRoles.asJava
  }

  override def toValue(): ValueMap = {
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

  def createBootstrapMember(user: TenantUser) = new CaseTeamMember(MemberKey(user.id, "user"), isOwner = Some(true))

  def deserialize(json: ValueMap) = {
    val memberId = json.get("memberId").getValue.toString
    val memberType = json.get("memberType").getValue.toString
    val isOwner = {
      val v = json.get("isOwner")
      if (v.equals(Value.NULL)) {
        None
      } else {
        Some(v.getValue.asInstanceOf[Boolean])
      }
    }

    import scala.collection.JavaConverters._
    val caseRoles = json.withArray("caseRoles").getValue.asScala.asInstanceOf[Seq[StringValue]].map(sv => sv.getValue)
    val removeRoles = json.withArray("removeRoles").getValue.asScala.asInstanceOf[Seq[StringValue]].map(sv => sv.getValue)
    new CaseTeamMember(MemberKey(memberId, memberType), caseRoles = caseRoles, isOwner = isOwner, removeRoles = removeRoles)
  }
}

