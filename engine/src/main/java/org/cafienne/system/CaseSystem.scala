/*
 * Copyright (C) 2014  Batav B.V.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.cafienne.system

import com.typesafe.scalalogging.LazyLogging
import org.apache.pekko.actor._
import org.cafienne.actormodel.identity.{CaseSystemIdentityRegistration, IdentityRegistration}
import org.cafienne.infrastructure.EngineVersion
import org.cafienne.infrastructure.config.CaseSystemConfig
import org.cafienne.infrastructure.config.util.SystemConfig
import org.cafienne.persistence.eventdb.EventDB
import org.cafienne.persistence.querydb.schema.QueryDB
import org.cafienne.storage.StorageCoordinator
import org.cafienne.system.bootstrap.BootstrapPlatformConfiguration
import org.cafienne.system.router.CaseEngineGateway
import org.cafienne.timerservice.TimerService

import scala.concurrent.ExecutionContextExecutor

/**
  *
  * A CaseSystem can be started either in Clustered mode, or as a Local system.
  * In the first case, it relies on actor clustering and sharding to manage the case instances
  * and forward messages to the proper case instance.
  * In the local scenario, the case system is run in-memory, and messages are forwarded by
  * a simple in-memory router.
  */
class CaseSystem(val systemConfig: SystemConfig, val system: ActorSystem, val queryDB: QueryDB, val eventDB: EventDB) extends LazyLogging {
  lazy val config: CaseSystemConfig = systemConfig.cafienne

  /**
   * Returns the BuildInfo as a string (containing JSON)
   */
  lazy val version = new EngineVersion

  implicit val ec: ExecutionContextExecutor = system.dispatcher

  /**
    * Retrieve a router for case messages. This will forward the messages to the correct case instance
    */
  val gateway: CaseEngineGateway = new CaseEngineGateway(this)

  // Create singleton actors
  val timerService: ActorRef = system.actorOf(Props.create(classOf[TimerService], this), TimerService.CAFIENNE_TIMER_SERVICE);

  val storageCoordinator = new StorageCoordinator(this)

  lazy val identityRegistration: IdentityRegistration = new CaseSystemIdentityRegistration(this)

  // First, start platform bootstrap configuration
  BootstrapPlatformConfiguration.run(this)
}

object CaseSystem {
  def DEFAULT: CaseSystem = apply(SystemConfig.DEFAULT)

  def apply(systemConfig: SystemConfig): CaseSystem = {
    val queryDB = new QueryDB(systemConfig.cafienne.persistence, systemConfig.cafienne.persistence.queryDB.jdbcConfig)
    val eventDB = new EventDB(systemConfig.cafienne.persistence)//, config.eventDB.jdbcConfig)
    new CaseSystem(systemConfig, ActorSystem("Cafienne-Case-System", systemConfig.config), queryDB, eventDB)
  }

  def apply(actorSystem: ActorSystem): CaseSystem = {
    val systemConfig = new SystemConfig(actorSystem.settings.config)
    val queryDB = new QueryDB(systemConfig.cafienne.persistence, systemConfig.cafienne.persistence.queryDB.jdbcConfig)
    val eventDB = new EventDB(systemConfig.cafienne.persistence)
    new CaseSystem(systemConfig, actorSystem, queryDB, eventDB)
  }
}
