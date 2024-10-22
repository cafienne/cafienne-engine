package org.cafienne.infrastructure.config.migration

import com.typesafe.config.Config
import org.cafienne.infrastructure.config.util.ConfigMigrator

object MigrateSerializer extends ConfigMigrator {
  override def run(config: Config): Config = {
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
}
