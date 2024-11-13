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

package com.casefabric.service.infrastructure.route

import org.apache.pekko.http.scaladsl.marshalling.Marshaller
import org.apache.pekko.http.scaladsl.model.headers.RawHeader
import org.apache.pekko.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import org.apache.pekko.http.scaladsl.server.Directives.{complete, optionalHeaderValueByName, respondWithHeader, respondWithHeaders}
import org.apache.pekko.http.scaladsl.server.{Directive0, Route}
import com.typesafe.scalalogging.LazyLogging
import com.casefabric.actormodel.response._
import com.casefabric.querydb.lastmodified.LastModifiedHeader

trait LastModifiedDirectives extends LazyLogging {
  /**
    * Simple CaseResponse converter to JSON
    */
  implicit val modelResponseMarshaller: Marshaller[ModelResponse, HttpEntity.Strict] = Marshaller.withFixedContentType(ContentTypes.`application/json`) { value: ModelResponse =>
    HttpEntity(ContentTypes.`application/json`, value.toJson.toString)
  }


  /**
    * Complete by marshalling the response as JSON and with writing last modified header
    */
  def completeWithLMH[R <: ModelResponse](statusCode: StatusCodes.Success, response: R, headerName: String): Route = {
    writeLastModifiedHeader(response, headerName) {
      complete(statusCode, response)
    }
  }

  /**
    * Complete without a response but still with writing last modified header
    */
  def completeOnlyLMH[R <: ModelResponse](statusCode: StatusCodes.Success, response: R, headerName: String): Route = {
    writeLastModifiedHeader(response, headerName) {
      complete(statusCode)
    }
  }

  def readLastModifiedHeader(headerName: String)(subRoute: LastModifiedHeader => Route): Route = {
    optionalHeaderValueByName(headerName) { value =>
      try {
        subRoute(LastModifiedHeader.get(headerName, value))
      } catch {
        case t: Throwable => // This happens if the header value does not comply with ActorLastModified format
          complete(StatusCodes.BadRequest, s"Header $headerName has invalid content. Reason: ${t.getMessage}")
      }
    }
  }

  def writeLastModifiedHeader(response: ModelResponse, header: String): Directive0 = {
    val lm = response.lastModifiedContent().toString
    if (lm != null) {
      respondWithHeader(RawHeader(header, response.lastModifiedContent.toString))
    } else {
      respondWithHeaders(Seq())
    }
  }
}
