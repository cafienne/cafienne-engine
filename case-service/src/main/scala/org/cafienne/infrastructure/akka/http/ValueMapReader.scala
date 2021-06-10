package org.cafienne.infrastructure.akka.http

import org.cafienne.actormodel.serialization.json.ValueMap

/**
  * Mechanism to easily read entity fields from a ValueMap
  */
object ValueMapReader {
  def read[T](valueMap: ValueMap, fieldName: String): T = {
    try {
      val value = valueMap.getValue.get(fieldName).getValue()
      value.asInstanceOf[T]
    } catch {
      case i: ClassCastException => {
        throw new RuntimeException("The value '" + fieldName + "' has a wrong type")
      }
      case n: NullPointerException => {
        throw new RuntimeException("The value '" + fieldName + "' is missing")
      }
    }
  }

  def read[T](valueMap: ValueMap, fieldName: String, defaultValue: T): T = {
    try {
      val value = valueMap.getValue.get(fieldName)
      if (value == null) {
        defaultValue
      } else {
        value.getValue().asInstanceOf[T]
      }
    } catch {
      case i: ClassCastException => {
        throw new RuntimeException("The value '" + fieldName + "' has a wrong type, expecting " + defaultValue.getClass.getSimpleName)
      }
      case n: NullPointerException => {
        throw new RuntimeException("The value '" + fieldName + "' is missing")
      }
    }
  }
}
