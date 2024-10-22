package org.cafienne.infrastructure.config.migration

import com.typesafe.config.Config
import org.cafienne.infrastructure.config.util.ConfigMigrator

object DropTagging extends ConfigMigrator {
  override def run(config: Config): Config = {
    // Tagging is configured in the akka persistence journal.
    //  This journal has different configuration keys per type of persistence.
    //  Find the right path based on the config of the journal plugin.
    val akkaJournalPath = config.root().toConfig.getString("akka.persistence.journal.plugin")
    println("Akka journal path: " + akkaJournalPath)
    val taggingPath = s"$akkaJournalPath.event-adapters"
    val key = "tagging"
    val deprecatedValue = "org.cafienne.akka.actor.tagging.CaseTaggingEventAdapter"
    val newValue = "org.cafienne.actormodel.tagging.CaseTaggingEventAdapter"
    val newConfig = dropConfigurationValue(config, taggingPath, key, deprecatedValue, newValue)

    val bindingPath = s"$akkaJournalPath.event-adapter-bindings"
    val newKey = "org.cafienne.actormodel.event.ModelEvent"
    dropConfigurationProperty(newConfig, bindingPath, newKey)
  }
}
