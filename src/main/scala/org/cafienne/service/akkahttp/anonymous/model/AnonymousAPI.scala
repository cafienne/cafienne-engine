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

package org.cafienne.service.akkahttp.anonymous.model

import io.swagger.v3.oas.annotations.media.Schema
import org.cafienne.infrastructure.akkahttp.EntityReader._
import org.cafienne.json.ValueMap

import scala.annotation.meta.field

object AnonymousAPI {

  implicit val anonymousStartCaseReader: EntityReader[AnonymousStartCaseFormat] = entityReader[AnonymousStartCaseFormat]


  @Schema(description = "Input parameters example json")
  case class InputParametersFormat(input1: String, input2: Object, input3: List[String])

  @Schema(description = "Start the execution of a new case")
  case class AnonymousStartCaseFormat(
                                       @(Schema@field)(
                                         description = "Input parameters that will be passed to the started case",
                                         required = false,
                                         implementation = classOf[InputParametersFormat])
                                       inputs: ValueMap,
                                       @(Schema@field)(description = "Unique identifier to be used for this case. When there is no identifier given, a UUID will be generated", required = false, example = "Will be generated if omitted or empty")
                                       caseInstanceId: Option[String],
                                       @(Schema@field)(description = "Indicator to start the case in debug mode", required = false, implementation = classOf[Boolean], example = "false")
                                       debug: Option[Boolean])

}
