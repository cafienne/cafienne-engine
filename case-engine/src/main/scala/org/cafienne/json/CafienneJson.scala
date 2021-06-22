package org.cafienne.json

import com.fasterxml.jackson.core.JsonGenerator
import org.cafienne.infrastructure.serialization.CafienneSerializable

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
}
