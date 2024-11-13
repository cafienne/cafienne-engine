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

package com.casefabric.system

import com.typesafe.scalalogging.LazyLogging
import org.apache.pekko.actor._
import com.casefabric.actormodel.identity.IdentityCache
import com.casefabric.infrastructure.CaseFabric
import com.casefabric.system.bootstrap.BootstrapPlatformConfiguration
import com.casefabric.system.router.CaseFabricGateway
import com.casefabric.timerservice.TimerService

import scala.concurrent.ExecutionContextExecutor

/**
  *
  * A CaseSystem can be started either in Clustered mode, or as a Local system.
  * In the first case, it relies on actor clustering and sharding to manage the case instances
  * and forward messages to the proper case instance.
  * In the local scenario, the case system is run in-memory, and messages are forwarded by
  * a simple in-memory router.
  */
class CaseSystem(val system: ActorSystem = ActorSystem("CaseFabric-Case-System", CaseFabric.config.systemConfig)) extends LazyLogging {

  implicit val ec: ExecutionContextExecutor = system.dispatcher

  /**
    * Retrieve a router for case messages. This will forward the messages to the correct case instance
    */
  val gateway: CaseFabricGateway = new CaseFabricGateway(this)

  // Create singleton actors
  val timerService: ActorRef = system.actorOf(Props.create(classOf[TimerService], this), TimerService.CASEFABRIC_TIMER_SERVICE);

  lazy val userCache: IdentityCache = new IdentityCache()

  // First, start platform bootstrap configuration
  BootstrapPlatformConfiguration.run(this)
}

