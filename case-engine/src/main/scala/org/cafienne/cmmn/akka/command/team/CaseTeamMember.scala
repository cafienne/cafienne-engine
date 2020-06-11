package org.cafienne.cmmn.akka.command.team

import org.cafienne.akka.actor.identity.TenantUser
import org.cafienne.cmmn.definition.CaseDefinition
import org.cafienne.cmmn.instance.casefile.{BooleanValue, StringValue, Value, ValueMap}
import org.cafienne.cmmn.instance.team.CaseTeamError

case class CaseTeamMember(key: MemberKey, caseRoles: Seq[String] = Seq(), isOwner: Option[Boolean] = None, removeRoles: Seq[String] = Seq()) {
  def validateRolesExist(caseDefinition: CaseDefinition): Unit = {
    val unfoundRole = (caseRoles ++ removeRoles).find(roleName => caseDefinition.getCaseRole(roleName) == null || roleName.isBlank)
    unfoundRole match {
      case Some(roleName) =>
        if (roleName.isBlank) throw new CaseTeamError("An empty role is not permitted")
        else throw new CaseTeamError("A role with name " + roleName + " is not defined in the case")
      case None => // cool
    }
  }

  def getCaseRoles = {
    import scala.collection.JavaConverters._
    caseRoles.asJava
  }

  def rolesToRemove = {
    import scala.collection.JavaConverters._
    removeRoles.asJava
  }

  def toValue(): ValueMap = {
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

