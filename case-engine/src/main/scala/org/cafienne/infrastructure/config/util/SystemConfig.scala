package org.cafienne.infrastructure.config.util

import com.typesafe.config.{Config, ConfigFactory, ConfigValue, ConfigValueFactory}
import com.typesafe.scalalogging.LazyLogging

/**
  * Static helper to load config settings for this JVM
  * Also migrates deprecated property values if found
  */
object SystemConfig extends LazyLogging {

  def load(): Config = {
    val fallback = ConfigFactory.defaultReference()
    val currentConfig = ConfigFactory.load().withFallback(fallback)
    val newConfig = migrateTagging(migrateSerializer(currentConfig))
    newConfig
  }

  def migrateSerializer(config: Config): Config = {
    val serializerPath = "akka.actor.serializers"
    val key = "cafienne_serializer"
    val deprecatedValue = "org.cafienne.akka.actor.serialization.CafienneSerializer"
    val newValue = "org.cafienne.infrastructure.serialization.CafienneSerializer"
    val newConfig = migrateConfigurationValue(config, serializerPath, key, deprecatedValue, newValue)

    val bindingPath = "akka.actor.serialization-bindings"
    val oldKey = "org.cafienne.akka.actor.serialization.CafienneSerializable"
    val newKey = "org.cafienne.infrastructure.serialization.CafienneSerializable"
    migrateConfigurationProperty(newConfig, bindingPath, oldKey, newKey)
  }

  def migrateTagging(config: Config): Config = {
    // Tagging is configured in the akka persistence journal.
    //  This journal has different configuration keys per type of persistence.
    //  Find the right path based on the config of the journal plugin.
    val akkaJournalPath = config.root().toConfig.getString("akka.persistence.journal.plugin")

    val taggingPath = s"$akkaJournalPath.event-adapters"
    val key = "tagging"
    val deprecatedValue = "org.cafienne.akka.actor.tagging.CaseTaggingEventAdapter"
    val newValue = "org.cafienne.actormodel.tagging.CaseTaggingEventAdapter"
    val newConfig = migrateConfigurationValue(config, taggingPath, key, deprecatedValue, newValue)

    val bindingPath = s"$akkaJournalPath.event-adapter-bindings"
    val oldKey = "org.cafienne.akka.actor.event.ModelEvent"
    val newKey = "org.cafienne.actormodel.event.ModelEvent"
    migrateConfigurationProperty(newConfig, bindingPath, oldKey, newKey)
  }

  /**
    * Print a big warning message, hopefully drawing attention :)
    * @param msg
    */
  def printWarning(msg: String) = {
    val extendedMessage = s"\tWARNING - $msg\t"
    val longestLine = extendedMessage.split("\n").map(_.length).max + 8 // Plus 8 for 2 tabs
    val manyHashes = List.fill(longestLine)('#').mkString
    // Print
    // - 2 blank lines
    // - many hashes
    // - 2 blank lines
    // - the actual message, preceded with "   WARNING - "
    // - 2 blank lines
    // - many hashes
    // - 2 blank lines
    logger.warn(s"\n\n$manyHashes\n\n$extendedMessage\n\n$manyHashes\n\n")
  }

  private def getLocationDescription(value: ConfigValue): String = {
    val origin = value.origin()
    s"${origin.url()}, line ${origin.lineNumber()}"
  }

  def migrateConfigurationValue(config: Config, path: String, key: String, oldValue: AnyRef, newValue: AnyRef): Config = {
    val keyPath = s"""$path.$key"""
    if (config.hasPath(keyPath)) {
      val configValue = config.getValue(keyPath)
      val location = getLocationDescription(configValue)
      val value = configValue.unwrapped()
      if (value != newValue) {
        if (value == oldValue) {
          printWarning(s"""$location\n\tPlease change deprecated configuration property '$keyPath' to\n\n\t\t$key = "$newValue" """)
          return config.withValue(keyPath, ConfigValueFactory.fromAnyRef(newValue))
        } else {
          printWarning(s"""$location\n\tConfiguration property '$keyPath' may have the wrong value; consider changing it to \n\n\t\t$key = "$newValue" """)
        }
      }
    }
    // Return the existing config
    config
  }

  /**
    * This migrates the old key to the new key.
    * Note that the key is string escaped inside the method and then gets appended to the path
    *
    * @param config
    * @param path
    * @param oldKey
    * @param newKey
    * @return
    */
  def migrateConfigurationProperty(config: Config, path: String, oldKey: String, newKey: String): Config = {
    val quotedOldKey = s""""$oldKey""""
    val quotedNewKey = s""""$newKey""""
    val oldPath = s"$path.$quotedOldKey"
    val newPath = s"$path.$quotedNewKey"

    if (!config.hasPath(newPath)) {
      if (config.hasPath(oldPath)) {
        val location = getLocationDescription(config.getValue(oldPath))
        val value = config.getAnyRef(oldPath)
        printWarning(s"""$location\n\tPlease change deprecated configuration property '$oldKey' to\n\n\t\t$newKey = $value""")

        // Replace the old property
        return config.withoutPath(oldPath).withValue(newPath, ConfigValueFactory.fromAnyRef(value))
      } else {
        printWarning(s"""Configuration property '$newKey' might be missing """)
      }
    }
    // Return the existing config
    config
  }
}



































