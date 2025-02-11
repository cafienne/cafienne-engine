package org.cafienne.persistence.flyway

import org.flywaydb.core.api.resolver.ResolvedMigration

import java.util
import scala.jdk.CollectionConverters.SeqHasAsJava

trait DBSchema {
  def scripts(tablePrefix: String): Seq[ResolvedMigration]

  def migrationScripts(tablePrefix: String): util.Collection[ResolvedMigration] = scripts(tablePrefix).asJava.asInstanceOf[java.util.Collection[ResolvedMigration]]
}
