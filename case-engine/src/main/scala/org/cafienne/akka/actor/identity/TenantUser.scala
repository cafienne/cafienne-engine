package org.cafienne.akka.actor.identity

import com.fasterxml.jackson.core.JsonGenerator
import org.cafienne.akka.actor.CaseSystem
import org.cafienne.akka.actor.serialization.AkkaSerializable
import org.cafienne.cmmn.instance.casefile.{Value, ValueMap}

import scala.collection.mutable

final case class TenantUser(id: String, roles: Seq[String], tenant: String, name: String, email: String = "", enabled: Boolean = true) extends AkkaSerializable {

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
    generator.writeEndObject()
  }

  def toJson = new ValueMap(
    Fields.userId, id,
    Fields.roles, roles.toArray,
    Fields.tenant, tenant,
    Fields.name, name,
    Fields.email, email)
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
    val roles = mutable.Set[String]()
    json.withArray(Fields.roles).forEach((value: Value[_]) => roles.add(value.getValue.toString))

    val rolesSet: Seq[String] = roles.toSeq

    TenantUser(id, rolesSet, tenant, name, email)
  }

  final def fromPlatformOwner(user: PlatformUser, tenantId: String): TenantUser = {
    if (!CaseSystem.isPlatformOwner(user.userId)) throw new SecurityException("Only platform owners can execute this type of command")
    TenantUser(user.userId, Seq(), tenantId, "")
  }

  /**
    * An empty TenantUser (can be used in invalid messages)
    */
  val NONE = TenantUser("", Seq(), "", "", "", false)
}