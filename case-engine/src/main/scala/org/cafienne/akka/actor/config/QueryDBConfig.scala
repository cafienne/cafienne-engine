package org.cafienne.akka.actor.config

import org.cafienne.akka.actor.config.util.{ChildConfigReader, MandatoryConfig}

import java.util.concurrent.TimeUnit
import scala.concurrent.duration.FiniteDuration


class QueryDBConfig(val parent: CafienneConfig) extends MandatoryConfig {
  override def path = "query-db"
  override val msg = "Cafienne Query Database is not configured. Check local.conf for 'cafienne.query-db' settings"

  lazy val restartSettings = new RestartConfig(this)
  lazy val debug = readBoolean("debug", false)
  lazy val readJournal = {
    logger.warn("Obtaining read-journal settings from 'cafienne.querydb.read-journal' is deprecated; please place these settings in 'cafienne.read-journal' instead")
    readString("read-journal")
  }
}

class RestartConfig(val parent: QueryDBConfig) extends ChildConfigReader {
  override def path = "restart-stream"

  lazy val minBackoff = readDuration("min-back-off", FiniteDuration(500, TimeUnit.MILLISECONDS))
  lazy val maxBackoff = readDuration("max-back-off", FiniteDuration(30, TimeUnit.SECONDS))
  lazy val randomFactor = readNumber("random-factor", 0.2).doubleValue()
  lazy val maxRestarts = readInt("max-restarts", 20)
}