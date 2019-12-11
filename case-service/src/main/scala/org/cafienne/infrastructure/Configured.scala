package org.cafienne.infrastructure

import com.typesafe.config.{Config, ConfigFactory}

trait Configured {
  def config: Config = defaultConfig

  private lazy val defaultConfig = {
    val fallback = ConfigFactory.defaultReference()
    ConfigFactory.load().withFallback(fallback)
  }
}
