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

package com.casefabric.service.http.cases.file

import io.swagger.v3.oas.annotations.media.Schema

import scala.annotation.meta.field

object CaseFileAPIFormat {
  @Schema(description = "JSON document containing the case file of a case instance")
  case class CaseFileJsonExampleFormat(Greeting: ExampleGreeting)

  case class ExampleGreeting(@(Schema @field)(example = "Hello world - Note: this is just an arbitrary case file example", implementation = classOf[String]) Message: String,
                             @(Schema @field)(example = "24-09-2031", implementation = classOf[String]) Date: String,
                             @(Schema @field)(example = "3", implementation = classOf[Integer]) Number: Integer,
                             @(Schema @field)(implementation = classOf[ExampleChild]) Child: ExampleChild)

  case class ExampleChild(@(Schema @field)(example = "true", implementation = classOf[Boolean]) Boolean: Boolean,
                          @(Schema @field)(example = "[1, 2, 3]", implementation = classOf[Object]) Array: Seq[Integer])
}
