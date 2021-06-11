package org.cafienne.service.api.model

import io.swagger.v3.oas.annotations.media.Schema
import org.cafienne.json.ValueMap

import scala.annotation.meta.field

@Schema(description = "Start the execution of a new case")
case class StartCaseFormat(
                            @(Schema@field)(
                              description = "Definition of the case to be started",
                              required = true,
                              example = "Depending on the internally configured DefinitionProvider this can be a file name or the case model itself.",
                              implementation = classOf[String])
                            definition: String = "", // by default an empty string to avoid nullpointers down the line
                            @(Schema@field)(
                              description = "Input parameters that will be passed to the started case",
                              required = false,
                              implementation = classOf[Examples.InputParametersFormat])
                            inputs: ValueMap,
                            @(Schema@field)(
                              description = "The team that will be connected to the execution of this case",
                              required = false,
                              implementation = classOf[Examples.StartCaseTeamFormat])
                            caseTeam: Option[BackwardCompatibleTeamFormat],
                            @(Schema@field)(description = "Tenant in which to create the case. If empty, default tenant as configured is taken.", required = false, implementation = classOf[Option[String]], example = "Will be taken from settings if omitted or empty")
                            tenant: Option[String],
                            @(Schema@field)(description = "Unique identifier to be used for this case. When there is no identifier given, a UUID will be generated", required = false, example = "Will be generated if omitted or empty")
                            caseInstanceId: Option[String],
                            @(Schema@field)(description = "Indicator to start the case in debug mode", required = false, implementation = classOf[Boolean], example = "false")
                            debug: Option[Boolean])
