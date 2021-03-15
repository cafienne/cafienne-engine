package org.cafienne.infrastructure.json

import com.fasterxml.jackson.core.JsonGenerator
import org.cafienne.akka.actor.serialization.CafienneSerializable
import org.cafienne.akka.actor.serialization.json.Value

/**
  * Simple trait that case classes can implement if they can convert themselves into Value[_] objects.
  * This can be used to make a case class json serializable (for now - better is to use default json serializers of akka http).
  */
trait CafienneJson extends CafienneSerializable {
  def toValue: Value[_]

  override def toString: String = toValue.toString

  override def write(generator: JsonGenerator): Unit = toValue.print(generator)
}
