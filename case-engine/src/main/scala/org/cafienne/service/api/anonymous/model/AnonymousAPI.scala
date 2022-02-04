package org.cafienne.service.api.anonymous.model

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
