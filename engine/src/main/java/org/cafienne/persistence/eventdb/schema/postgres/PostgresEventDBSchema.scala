package org.cafienne.persistence.eventdb.schema.postgres

import org.cafienne.persistence.eventdb.schema.EventDBSchema
import org.cafienne.persistence.flyway.SchemaMigrator

object PostgresEventDBSchema extends EventDBSchema {
  override def scripts(): Seq[SchemaMigrator] = Seq(new V1_1_13__AddTimerService(), new V1_1_16__CreateAkkaSchema())
}
