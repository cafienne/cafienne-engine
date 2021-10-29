package org.cafienne.tenant.actorapi.command

import com.fasterxml.jackson.core.JsonGenerator
import org.cafienne.infrastructure.serialization.Fields
import org.cafienne.json._

final case class TenantUserInformation(id: String, roles: Option[Seq[String]] = None, name: Option[String] = None, email: Option[String] = None, owner: Option[Boolean] = None, enabled: Option[Boolean] = None) extends CafienneJson {

  def getName(): String = name.getOrElse("")
  def getEmail(): String = email.getOrElse("")
  def getRoles(): Seq[String] = roles.getOrElse(Seq())
  def isOwner(): Boolean = owner.getOrElse(false)
  def isEnabled(): Boolean = enabled.getOrElse(true)

  /**
    * Serializes the user information to JSON
    *
    * @param generator
    */
  override def write(generator: JsonGenerator): Unit = {
    generator.writeStartObject()
    writeField(generator, Fields.userId, id)
    // Write optional fields through foreach pattern
    writeStringField(generator, Fields.name, name)
    writeStringField(generator, Fields.email, email)
    writeListField(generator, Fields.roles, roles)
    writeBooleanField(generator, Fields.isOwner, owner)
    writeBooleanField(generator, Fields.enabled, enabled)
    generator.writeEndObject()
  }

  override def writeThisObject(generator: JsonGenerator): Unit = super.writeThisObject(generator)

  override def toValue: Value[_] = {
    val json: ValueMap = new ValueMap(Fields.userId, id)
    putStringField(json, Fields.name, name)
    putStringField(json, Fields.email, email)
    putBooleanField(json, Fields.isOwner, owner)
    putBooleanField(json, Fields.enabled, enabled)
    putStringList(json, Fields.roles, roles)
    json
  }
}

object TenantUserInformation {
  def from(json: ValueMap) : TenantUserInformation = {
    val userId: String = json.readString(Fields.userId)
    val roles = CafienneJson.readOptionalStringList(json, Fields.roles)
    val name = CafienneJson.readOptionalString(json, Fields.name)
    val email = CafienneJson.readOptionalString(json, Fields.email)
    val owner = CafienneJson.readOptionalBoolean(json, Fields.isOwner)
    val enabled = CafienneJson.readOptionalBoolean(json, Fields.enabled)

    TenantUserInformation(userId, roles, name, email, owner, enabled)
  }
}
