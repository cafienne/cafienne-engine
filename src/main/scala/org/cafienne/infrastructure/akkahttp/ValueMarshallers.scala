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

import akka.http.scaladsl.marshalling.Marshaller
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshaller
import org.cafienne.json.{JSONReader, Value, ValueList, ValueMap}
import org.cafienne.util.XMLHelper
import org.w3c.dom.Document

/**
  * This file contains some marshallers and unmarshallers for Value objects and XML documents
  */
object ValueMarshallers {

  /**
    * Application/xml is not a default in the {@link akka.http.scaladsl.model.ContentTypes}
    */
  val `application/xml` : ContentType = MediaTypes.`application/xml` withCharset HttpCharsets.`UTF-8`

  /**
    * Reads an org.w3c.dom.Document from a http entity
    */
  implicit val DocumentUnmarshaller = Unmarshaller.stringUnmarshaller.forContentTypes(`application/xml`).map(data => {
    XMLHelper.loadXML(data.getBytes())
  })

  implicit val ValueUnmarshaller = Unmarshaller.stringUnmarshaller.forContentTypes(ContentTypes.`application/json`).map(data => {
    JSONReader.parse(data).asInstanceOf[Value[_]]
  })

  implicit val ValueMapUnmarshaller = Unmarshaller.stringUnmarshaller.forContentTypes(ContentTypes.`application/json`).map(data => {
    JSONReader.parse(data).asInstanceOf[ValueMap]
  })

  implicit val ValueListUnmarshaller = Unmarshaller.stringUnmarshaller.forContentTypes(ContentTypes.`application/json`).map(data => {
    JSONReader.parse(data).asInstanceOf[ValueList]
  })

  /**
    * Writes an org.w3c.dom.Document to a http entity
    */
  implicit val documentMarshaller = Marshaller.withFixedContentType(`application/xml`){ value: Document =>
    HttpEntity(ContentTypes.`text/xml(UTF-8)`, XMLHelper.printXMLNode(value))
  }

  implicit val valueMapMarshaller = Marshaller.withFixedContentType(ContentTypes.`application/json`) { value: ValueMap =>
    HttpEntity(ContentTypes.`application/json`, value.toString)
  }

  implicit val valueListMarshaller = Marshaller.withFixedContentType(ContentTypes.`application/json`) { value: ValueList =>
    HttpEntity(ContentTypes.`application/json`, value.toString)
  }
}
