package org.cafienne.persistence.eventdb.schema.hsqldb

import org.cafienne.infrastructure.Cafienne
import org.cafienne.persistence.eventdb.schema.ClassicEventDBSchemaScript

class V1_1_20__CreateAkkaSchema extends ClassicEventDBSchemaScript {
  val version = "1.1.20"

  val description = "CreateAkkaSchemaOrderingIndex"

  val scriptName = "org.cafienne.persistence.schema.V1_1_20__CreateAkkaSchemaOrderingIndex"

  override def getChecksum: Integer = 1702145594

  override def sql: String = {
    s"""CREATE UNIQUE INDEX "PUBLIC"."${Cafienne.config.persistence.tablePrefix}event_journal_ordering_idx" ON "PUBLIC"."${Cafienne.config.persistence.tablePrefix}event_journal" (ordering);""".stripMargin
  }
 }
