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
import com.casefabric.util.XMLHelper
import org.w3c.dom.Document

/**
  * This file contains an un-marshaller for XML documents
  */
object HttpXmlReader {

  /**
    * Application/xml is not a default in the [[pekko.http.scaladsl.model.ContentTypes]]
    */
  val `application/xml`: ContentType = MediaTypes.`application/xml` withCharset HttpCharsets.`UTF-8`

  /**
    * Reads an org.w3c.dom.Document from a http entity
    */
  implicit val DocumentUnmarshaller: Unmarshaller[HttpEntity, Document] = Unmarshaller.stringUnmarshaller.forContentTypes(`application/xml`).map(XMLHelper.loadXML)
}
