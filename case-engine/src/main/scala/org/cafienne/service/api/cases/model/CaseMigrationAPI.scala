package org.cafienne.service.api.cases.model

import io.swagger.v3.oas.annotations.media.Schema
import org.cafienne.infrastructure.akka.http.EntityReader.{EntityReader, entityReader}

import scala.annotation.meta.field

object CaseMigrationAPI {
  implicit val migrationReader: EntityReader[MigrationDefinitionFormat] = entityReader[MigrationDefinitionFormat]

  @Schema(description = "Migrate definition of a case")
  case class MigrationDefinitionFormat(
                                        @(Schema@field)(description = "New definition of the case to be migrated", required = true, example = "Depending on the internally configured DefinitionProvider this can be a file name or the case model itself.", implementation = classOf[String])
                                        newDefinition: String = "", // by default an empty string to avoid nullpointers down the line
                                      )

}
