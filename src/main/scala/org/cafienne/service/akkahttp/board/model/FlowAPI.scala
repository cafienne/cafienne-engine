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

package org.cafienne.service.akkahttp.board.model

import org.cafienne.infrastructure.akkahttp.EntityReader.{EntityReader, entityReader}

object FlowAPI {
  implicit val startFlowFormatReader: EntityReader[StartFlowFormat] = entityReader[StartFlowFormat]
  implicit val completeFlowTaskFormatReader: EntityReader[FlowTaskOutputFormat] = entityReader[FlowTaskOutputFormat]

  case class StartFlowFormat(id: Option[String], subject: String, data: Option[Map[String, _]])

  case class FlowStartedFormat(flowId: String)

  case class FlowTaskOutputFormat(subject: String, data: Map[String, _])
}