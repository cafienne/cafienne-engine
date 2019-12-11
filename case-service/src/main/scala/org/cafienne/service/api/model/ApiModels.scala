/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.service.api.model

import java.time.Instant

import io.swagger.v3.oas.annotations.media.Schema
import org.cafienne.cmmn.akka.command.team.CaseTeam
import org.cafienne.cmmn.instance.casefile.ValueMap

import scala.annotation.meta.field

final object Examples {
  @Schema(description = "Input parameters example json")
  case class InputParameters(input1: String, input2: Object)

  @Schema(description = "Output parameters example json")
  case class OutputParameters(output1: String, output2: Object, output3: List[String])
}

@Schema(description = "Start the execution of a new case")
case class StartCase(
                      @(Schema @field)(description = "Definition of the case to be started", required = true, implementation = classOf[String]) definition: String,
                      @(Schema @field)(description = "Input parameters that will be passed to the started case", required = false, implementation = classOf[Examples.InputParameters]) inputs: ValueMap,
                      @(Schema @field)(description = "The team that will be connected to the execution of this case", required = false, implementation = classOf[CaseTeam]) caseTeam: CaseTeam,
                      @(Schema @field)(description = "Tenant in which to create the case. If empty, default tenant as configured is taken.", required = false, implementation = classOf[Option[String]], example = "Will be taken from settings if omitted or empty") tenant: Option[String] = None,
                      @(Schema @field)(description = "Unique identifier to be used for this case. When there is no identifier given, a UUID will be generated", required = false, example = "Will be generated if omitted or empty") caseInstanceId: Option[String],
                      @(Schema @field)(description = "Indicator to start the case in debug mode", required = false, implementation = classOf[Boolean], example = "false") debug: Option[Boolean] = Some(false))

// CaseFileItem
case class CaseFileItem(id: String,
                        name: String,
                        caseInstanceId: String,
                        properties: Option[List[Property]],
                        parentCaseFileItemId: String,
                        content: String,
                        mimetype: String,
                        transition: String,
                        lastModified: Instant)


//base types
// Property of a caseFileItem
case class Property(name: String, `type`: String, value: String)
