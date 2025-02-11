package org.cafienne.persistence.eventdb.schema.hsqldb

import org.cafienne.infrastructure.Cafienne
import org.cafienne.persistence.eventdb.schema.ClassicEventDBSchemaScript

class V1_1_18__CreateAkkaSchema extends ClassicEventDBSchemaScript {
  val version = "1.1.18"

  val description = "CreateAkkaSchemaTagIndex"

  val scriptName = "org.cafienne.persistence.schema.V1_1_18__CreateAkkaSchemaTagIndex"

  override def getChecksum: Integer = 1702145592

  override def sql: String = {
    s"""CREATE INDEX "PUBLIC"."${Cafienne.config.persistence.tablePrefix}state_tag_idx" on "PUBLIC"."${Cafienne.config.persistence.tablePrefix}durable_state" (tag);""".stripMargin
  }
}
