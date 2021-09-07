package org.cafienne.json

import com.fasterxml.jackson.core.JsonGenerator
import org.cafienne.infrastructure.serialization.{CafienneSerializable, Fields}

/**
  * Simple trait that case classes can implement if they can convert themselves into Value[_] objects.
  * This can be used to make a case class json serializable (for now - better is to use default json serializers of akka http).
  */
trait CafienneJson extends CafienneSerializable {
  def toValue: Value[_]

  override def toString: String = toValue.toString

  override def write(generator: JsonGenerator): Unit = {
    toValue.print(generator)
  }

  def writeListField(generator: JsonGenerator, fieldName: Fields, stringList: Option[Seq[String]]): Unit = {
    import scala.jdk.CollectionConverters._
    stringList.foreach(value => writeField(generator, fieldName, value.asJava))
  }

  def writeStringField(generator: JsonGenerator, fieldName: Fields, string: Option[String]): Unit = {
    string.foreach(value => writeField(generator, fieldName, value))
  }

  def writeBooleanField(generator: JsonGenerator, fieldName: Fields, bool: Option[Boolean]): Unit = {
    bool.foreach(value => writeField(generator, fieldName, value))
  }

  def putStringList(json: ValueMap, fieldName: Fields, stringList: Option[Seq[String]]): Unit = {
    stringList.foreach(value => json.put(fieldName, Value.convert(value)))
  }

  def putStringField(json: ValueMap, fieldName: Fields, string: Option[String]): Unit = {
    string.map(value => json.put(fieldName, new StringValue(value)))
  }

  def putBooleanField(json: ValueMap, fieldName: Fields, bool: Option[Boolean]): Unit = {
    bool.map(value => json.put(fieldName, new BooleanValue(value)))
  }
}

object CafienneJson {
  import scala.jdk.CollectionConverters._

  def readOptionalStringList(json: ValueMap, field: Fields): Option[Seq[String]] = {
    json.get(field) match {
      case list: ValueList => Some(list.getValue.asScala.toSeq.map(v => v.getValue.asInstanceOf[String]))
      case _ => None
    }
  }

  def readOptionalString(json: ValueMap, field: Fields): Option[String] = json.get(field) match {
    case value: StringValue => Some(value.getValue)
    case _ => None
  }

  def readOptionalBoolean(json: ValueMap, field: Fields): Option[Boolean] = json.get(field) match {
    case value: BooleanValue => Some(value.getValue)
    case _ => None
  }
}
