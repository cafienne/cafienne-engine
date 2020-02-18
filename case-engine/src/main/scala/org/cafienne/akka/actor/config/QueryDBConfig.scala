package org.cafienne.akka.actor.config

import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging


class QueryDBConfig(val parentConfig: Config) extends LazyLogging {

  lazy val config = {
    if (parentConfig.hasPath("query-db")) {
      parentConfig.getConfig("query-db")
    } else {
      throw new IllegalArgumentException("Cafienne Query Database is not configured. Check local.conf for 'cafienne.query-db' settings")
    }
  }

  lazy val debug = {
    if (config.hasPath("debug")) {
      config.getBoolean("debug")
    }
    false
  }

  lazy val readJournal = {
    if (config.hasPath("read-journal")) {
      val explicitReadJournal = config.getString("read-journal")
      logger.debug("Using explicit read journal configuration reference: " + explicitReadJournal)
      explicitReadJournal
    } else {
      ""
    }
  }
}
