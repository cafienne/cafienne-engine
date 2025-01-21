package org.cafienne.infrastructure.config.persistence.querydb

import org.apache.pekko.stream.RestartSettings
import org.cafienne.infrastructure.config.util.ChildConfigReader

import java.util.concurrent.TimeUnit
import scala.concurrent.duration.FiniteDuration

class RestartConfig(val parent: QueryDBConfig) extends ChildConfigReader {
  def path = "restart-stream"

  lazy val minBackoff: FiniteDuration = readDuration("min-back-off", FiniteDuration(500, TimeUnit.MILLISECONDS))
  lazy val maxBackoff: FiniteDuration = readDuration("max-back-off", FiniteDuration(30, TimeUnit.SECONDS))
  lazy val randomFactor: Double = readNumber("random-factor", 0.2).doubleValue()
  lazy val maxRestarts: Int = readInt("max-restarts", 20)
  lazy val maxRestartsWithin: FiniteDuration = readDuration("max-restarts-within", FiniteDuration(5, TimeUnit.MINUTES))

  lazy val settings: RestartSettings = RestartSettings(minBackoff, maxBackoff, randomFactor).withMaxRestarts(maxRestarts, maxRestartsWithin)
}
