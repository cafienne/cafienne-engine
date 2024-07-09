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

import org.apache.pekko.http.scaladsl.model.{ContentTypes, HttpEntity}
import org.apache.pekko.http.scaladsl.unmarshalling.Unmarshaller
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.module.scala.{ClassTagExtensions, DefaultScalaModule}
import org.cafienne.infrastructure.serialization.{ValueMapJacksonDeserializer, ValueMapJacksonSerializer}
import org.cafienne.json.ValueMap

/**
  * Helper classes to read http entity as an expected type
  */
object EntityReader {
  type EntityReader[T] = Unmarshaller[HttpEntity, T]

  private val mapper = new ObjectMapper() with ClassTagExtensions
  mapper.registerModule(DefaultScalaModule)

  val valueMapModule = new SimpleModule
  valueMapModule.addSerializer(classOf[ValueMap], new ValueMapJacksonSerializer)
  valueMapModule.addDeserializer(classOf[ValueMap], new ValueMapJacksonDeserializer)
  mapper.registerModule(valueMapModule)

  /**
    * Converts the incoming data to json and from there to a typed entity
    * @param m
    * @tparam T
    * @return
    */
  def entityReader[T](implicit m : Manifest[T]): Unmarshaller[HttpEntity, T] = Unmarshaller.stringUnmarshaller.forContentTypes(ContentTypes.`application/json`).map(data => mapper.readValue[T](data))
}
