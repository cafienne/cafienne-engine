package org.cafienne.authentication

import com.nimbusds.jwt.JWTClaimsSet
import org.cafienne.actormodel.exception.MissingTenantException
import org.cafienne.actormodel.identity.UserIdentity
import org.cafienne.infrastructure.Cafienne
import org.cafienne.infrastructure.serialization.Fields
import org.cafienne.json.{Value, ValueMap}

class AuthenticatedUser(val token: String, claims: JWTClaimsSet) extends UserIdentity {
  val id: String = claims.getSubject

  def resolveTenant(optionalTenant: Option[String]): String = {
    val tenant = optionalTenant match {
      case Some(value) => if (value.isBlank) Cafienne.config.platform.defaultTenant else value
      case None => Cafienne.config.platform.defaultTenant
    }
    if (tenant.isBlank) {
      throw new MissingTenantException("Tenant field is empty or missing and a default tenant is not configured")
    }
    tenant
  }

  override def toValue: Value[_] = new ValueMap(Fields.userId, id)
}

object AuthenticatedUser {
  def deserialize(json: ValueMap): AuthenticatedUser = {
    // Throw an error, as probably this code is not yet in use either
    throw new NullPointerException("Deserialization of AuthenticatedUser is not yet implemented")
  }
}
