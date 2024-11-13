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

package com.casefabric.service.infrastructure.payload

import org.apache.pekko.http.scaladsl.model._
import org.apache.pekko.http.scaladsl.unmarshalling.Unmarshaller
import com.casefabric.json.{JSONReader, Value, ValueList, ValueMap}

/**
  * This file contains unmarshallers for various types of Value[_] objects
  */
object HttpJsonReader {

  implicit val ValueUnmarshaller: Unmarshaller[HttpEntity, Value[_]] = Unmarshaller.stringUnmarshaller.forContentTypes(ContentTypes.`application/json`).map(JSONReader.parse(_).asInstanceOf[Value[_]])

  implicit val ValueMapUnmarshaller: Unmarshaller[HttpEntity, ValueMap] = Unmarshaller.stringUnmarshaller.forContentTypes(ContentTypes.`application/json`).map(JSONReader.parse(_).asInstanceOf[ValueMap])

  implicit val ValueListUnmarshaller: Unmarshaller[HttpEntity, ValueList] = Unmarshaller.stringUnmarshaller.forContentTypes(ContentTypes.`application/json`).map(JSONReader.parse(_).asInstanceOf[ValueList])
}
