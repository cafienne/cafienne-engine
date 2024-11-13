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

package com.casefabric.service.http.cases.documentation

import io.swagger.v3.oas.annotations.media.Schema

import scala.annotation.meta.field

object DocumentationAPIFormat {
  @Schema(description = "Documentation of an element in a case instance")
  case class DocumentationResponseFormat(@(Schema @field)(
                                  description = "Format of the text in the documentation, e.g. 'text/plain' or 'text/html'",
                                  example = "text/plain",
                                  implementation = classOf[String])
                                textFormat: String,
                                @(Schema @field)(
                                  description = "Actual documentation text",
                                  example = "This field contains the actual documentation of the element",
                                  implementation = classOf[String])
                                text: String
                               )

  @Schema(description = "Documentation of the file in a case instance")
  case class CaseFileDocumentationFormat(@(Schema @field)(
                                          description = "The case file path element",
                                          example = "Greeting/Message",
                                          implementation = classOf[String])
                                         path: String,
                                         @(Schema @field)(
                                           description = "Actual documentation text",
                                           implementation = classOf[DocumentationResponseFormat])
                                         documentation: DocumentationResponseFormat
                                        )
}
