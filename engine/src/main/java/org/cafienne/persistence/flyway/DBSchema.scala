package org.cafienne.persistence.flyway

import org.flywaydb.core.api.resolver.ResolvedMigration

import java.util
import scala.jdk.CollectionConverters.SeqHasAsJava

trait DBSchema {
  def scripts(): Seq[ResolvedMigration]

  def migrationScripts(): util.Collection[ResolvedMigration] = scripts().asJava.asInstanceOf[java.util.Collection[ResolvedMigration]]
}
