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

package org.cafienne.service.http.repository

import io.swagger.v3.oas.annotations.media.Schema

import scala.annotation.meta.field

object RepositoryAPIFormat {
  @Schema(description = "List of models in the repository of the engine")
  case class ModelListResponseFormat(@(Schema @field)(description = "List of case models") models: Seq[ModelResponseFormat])

  case class ModelResponseFormat(@(Schema @field)(description = "Name of the case model", example = "helloworld.xml") definitions: String,
                                 @(Schema @field)(description = "Description of the case definition", example = "Greetings to the world") description: String)

}
