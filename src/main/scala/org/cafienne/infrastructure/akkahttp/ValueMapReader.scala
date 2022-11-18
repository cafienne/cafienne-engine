/*
 * Copyright (C) 2014  Batav B.V.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.cafienne.infrastructure.akkahttp

import org.cafienne.json.ValueMap

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
