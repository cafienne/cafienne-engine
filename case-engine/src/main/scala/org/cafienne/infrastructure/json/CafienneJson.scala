package org.cafienne.infrastructure.json

import org.cafienne.actormodel.serialization.json.Value

/**
  * Simple trait that case classes can implement if they can convert themselves into Value[_] objects.
  * This can be used to make a case class json serializable (for now - better is to use default json serializers of akka http).
  */
trait CafienneJson {
  def toValue: Value[_]

  override def toString: String = toValue.toString
}
