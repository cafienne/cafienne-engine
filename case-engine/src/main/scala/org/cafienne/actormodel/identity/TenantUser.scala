package org.cafienne.actormodel.identity

import com.fasterxml.jackson.core.JsonGenerator
import org.cafienne.actormodel.exception.AuthorizationException
import org.cafienne.infrastructure.Cafienne
import org.cafienne.infrastructure.serialization.Fields
import org.cafienne.json.{BooleanValue, CafienneJson, Value, ValueMap}

final case class TenantUser(id: String, roles: Seq[String], tenant: String, isOwner: Boolean = false, name: String, email: String = "", enabled: Boolean = true) extends CafienneJson {

  import scala.jdk.CollectionConverters._

  /**
    * Serializes the user information to JSON
    *
    * @param generator
    */
  override def write(generator: JsonGenerator): Unit = {
    generator.writeStartObject()
    writeField(generator, Fields.userId, id)
    writeField(generator, Fields.roles, roles.asJava)
    writeField(generator, Fields.tenant, tenant)
    writeField(generator, Fields.name, name)
    writeField(generator, Fields.email, email)
    writeField(generator, Fields.isOwner, isOwner)
    generator.writeEndObject()
  }

  override def toValue: Value[_] = new ValueMap(
    Fields.userId, id,
    Fields.roles, roles.toArray,
    Fields.tenant, tenant,
    Fields.name, name,
    Fields.email, email,
    Fields.isOwner, new BooleanValue(isOwner))
}

object TenantUser {
  /**
    * Deserialize the json into a user context
    *
    * @param json
    * @return instance of user context
    */
  def from(json: ValueMap): TenantUser = {
    val name: String = json.readString(Fields.name, "")
    val id: String = json.readString(Fields.userId)
    val email: String = json.readString(Fields.email, "")
    val tenant: String = json.readString(Fields.tenant)
    val isOwner: Boolean = json.readBoolean(Fields.isOwner, false)
    val roles = json.readStringList(Fields.roles).toSeq

    TenantUser(id, roles, tenant, isOwner, name, email)
  }

  final def fromPlatformOwner(user: PlatformUser, tenantId: String): TenantUser = {
    if (!Cafienne.isPlatformOwner(user.userId)) throw AuthorizationException("Only platform owners can execute this type of command")
    TenantUser(user.userId, Seq(), tenantId, name = "")
  }

  /**
    * An empty TenantUser (can be used in invalid messages)
    */
  val NONE = TenantUser("", Seq(), "", name = "", email = "", enabled = false)
}