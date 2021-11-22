package org.cafienne.actormodel.identity

import org.cafienne.infrastructure.serialization.Fields
import org.cafienne.json.{Value, ValueMap}

trait CaseUserIdentity extends UserIdentity {
  // user id
  val id: String
  // user origin (IDP, Tenant, Platform, etc.)
  val origin: Origin
  // tenant roles this user has, defaults to empty
  val tenantRoles: Set[String] = Set()

  override def toValue: Value[_] = {
    super.toValue.asMap().plus(Fields.origin, origin, Fields.tenantRoles, tenantRoles)
  }

  override def asCaseUserIdentity(): CaseUserIdentity = this
}

object CaseUserIdentity {
  def deserialize(json: ValueMap): CaseUserIdentity = {
    new CaseUserIdentity {
      override val id: String = json.readString(Fields.userId)
      override val origin: Origin = json.readEnum(Fields.origin, classOf[Origin])
      override val tenantRoles: Set[String] = json.readStringList(Fields.tenantRoles).toSet
    }
  }
}
