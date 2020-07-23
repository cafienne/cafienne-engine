package org.cafienne.akka.actor.identity

import com.fasterxml.jackson.core.JsonGenerator
import org.cafienne.akka.actor.CaseSystem
import org.cafienne.akka.actor.command.exception.AuthorizationException
import org.cafienne.akka.actor.serialization.Fields
import org.cafienne.akka.actor.serialization.AkkaSerializable
import org.cafienne.akka.actor.serialization.json.{BooleanValue, Value, ValueMap}
import org.cafienne.infrastructure.json.CafienneJson

import scala.collection.mutable

final case class TenantUser(id: String, roles: Seq[String], tenant: String, isOwner: Boolean = false, name: String, email: String = "", enabled: Boolean = true) extends AkkaSerializable with CafienneJson {

  import scala.collection.JavaConverters._

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

  override def toValue = new ValueMap(
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
    val name: String = json.raw(Fields.name)
    val id: String = json.raw(Fields.userId)
    val email: String = json.raw(Fields.email)
    val tenant: String = json.raw(Fields.tenant)
    val isOwner: Boolean = {
      if (json.has(Fields.isOwner.toString)) json.raw(Fields.isOwner)
      else false
    }
    val roles = mutable.Set[String]()
    json.withArray(Fields.roles).forEach((value: Value[_]) => roles.add(value.getValue.toString))

    val rolesSet: Seq[String] = roles.toSeq

    TenantUser(id, rolesSet, tenant, isOwner, name, email)
  }

  final def fromPlatformOwner(user: PlatformUser, tenantId: String): TenantUser = {
    if (!CaseSystem.isPlatformOwner(user.userId)) throw AuthorizationException("Only platform owners can execute this type of command")
    TenantUser(user.userId, Seq(), tenantId, name = "")
  }

  /**
    * An empty TenantUser (can be used in invalid messages)
    */
  val NONE = TenantUser("", Seq(), "", name = "", email = "", enabled = false)
}