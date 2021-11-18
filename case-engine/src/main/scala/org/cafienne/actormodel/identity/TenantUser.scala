package org.cafienne.actormodel.identity

import com.fasterxml.jackson.core.JsonGenerator
import org.cafienne.infrastructure.serialization.Fields
import org.cafienne.json.{Value, ValueMap}

final case class TenantUser(id: String, roles: Set[String], tenant: String, isOwner: Boolean = false, name: String, email: String = "", enabled: Boolean = true) extends UserIdentity {

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
    Fields.isOwner, isOwner)
}

object TenantUser {
  /**
    * Deserialize the json into a user context
    *
    * @param json
    * @return instance of user context
    */
  def deserialize(json: ValueMap): TenantUser = {
    val id: String = json.readString(Fields.userId)
    val name: String = json.readString(Fields.name, "")
    val email: String = json.readString(Fields.email, "")
    val tenant: String = json.readString(Fields.tenant)
    val isOwner: Boolean = json.readBoolean(Fields.isOwner)
    val roles = json.readStringList(Fields.roles).toSet

    TenantUser(id, roles, tenant, isOwner, name, email)
  }
}
