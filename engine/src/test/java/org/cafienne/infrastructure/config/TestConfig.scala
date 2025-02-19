package org.cafienne.infrastructure.config

import com.typesafe.config.{Config, ConfigFactory}

object TestConfig {
  val config: Config = ConfigFactory.load("application.conf")
}
