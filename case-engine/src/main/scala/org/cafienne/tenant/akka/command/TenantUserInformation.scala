package org.cafienne.tenant.akka.command

import com.fasterxml.jackson.core.JsonGenerator
import org.cafienne.akka.actor.serialization.json.{BooleanValue, StringValue, ValueList, ValueMap}
import org.cafienne.akka.actor.serialization.{CafienneSerializable, Fields}
import org.cafienne.infrastructure.json.CafienneJson

import scala.jdk.CollectionConverters._

final case class TenantUserInformation(id: String, roles: Option[Seq[String]] = None, name: Option[String] = None, email: Option[String] = None, owner: Option[Boolean] = None, enabled: Option[Boolean] = None) extends CafienneSerializable with CafienneJson {

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
    name.map(name => writeField(generator, Fields.name, name))
    email.map(email => writeField(generator, Fields.email, email))
    roles.map(roles => writeField(generator, Fields.roles, roles.asJava))
    owner.map(value => writeField(generator, Fields.isOwner, value))
    enabled.map(value => writeField(generator, Fields.enabled, value))
    generator.writeEndObject()
  }

  override def toValue = {
    val json: ValueMap = new ValueMap(Fields.userId, id)
    name.map(name => json.put(Fields.name, new StringValue(name)))
    email.map(email => json.put(Fields.email, new StringValue(email)))
    owner.map(owner => json.put(Fields.isOwner, new BooleanValue(owner)))
    enabled.map(enabled => json.put(Fields.enabled, new BooleanValue(enabled)))

    roles.foreach(roles => {
      val list: ValueList = json.withArray(Fields.roles)
      roles.foreach(role => list.add(new StringValue(role)))
    })

    json
  }
}

object TenantUserInformation {
  def from(json: ValueMap) : TenantUserInformation = {
    def readOptionalStringList(field:Fields): Option[Seq[String]] = {
      json.get(field) match {
        case list: ValueList => Some(list.getValue.asScala.toSeq.map(v => v.getValue.asInstanceOf[String]))
        case _ => None
      }
    }
    def readOptionalString(field: Fields): Option[String] = json.get(field) match {
      case value: StringValue => Some(value.getValue)
      case _ => None
    }
    def readOptionalBoolean(field: Fields): Option[Boolean] = json.get(field) match {
      case value: BooleanValue => Some(value.getValue)
      case _ => None
    }

    val userId: String = json.raw(Fields.userId)
    val tenant: String = json.raw(Fields.tenant)
    val roles = readOptionalStringList(Fields.roles)
    val name = readOptionalString(Fields.name)
    val email = readOptionalString(Fields.email)
    val owner = readOptionalBoolean(Fields.isOwner)
    val enabled = readOptionalBoolean(Fields.enabled)

    TenantUserInformation(userId, roles, name, email, owner, enabled)

  }
}
