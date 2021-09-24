package org.cafienne.service.api.model

import io.swagger.v3.oas.annotations.media.Schema

import scala.annotation.meta.field

@Schema(description = "Migrate definition of a case")
case class MigrationDefinitionFormat(
    @(Schema@field)(description = "New definition of the case to be migrated", required = true, example = "Depending on the internally configured DefinitionProvider this can be a file name or the case model itself.", implementation = classOf[String])
  newDefinition: String = "", // by default an empty string to avoid nullpointers down the line
)
