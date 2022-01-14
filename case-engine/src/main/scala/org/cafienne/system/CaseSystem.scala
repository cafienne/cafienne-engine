/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.system

import akka.actor._
import com.typesafe.scalalogging.LazyLogging
import org.cafienne.infrastructure.Cafienne
import org.cafienne.system.bootstrap.BootstrapPlatformConfiguration
import org.cafienne.system.router.CafienneGateway
import org.cafienne.timerservice.TimerService

/**
  *
  * A CaseSystem can be started either in Clustered mode, or as a Local system.
  * In the first case, it relies on Akka clustering and sharding to manage the case instances
  * and forward messages to the proper case instance.
  * In the local scenario, the case system is run in-memory, and messages are forwarded by
  * a simple in-memory router.
  */
class CaseSystem(val name: String = "Cafienne-Case-System") extends LazyLogging {
  /**
    * Start the Case System. This will spin up an akka system according to the specifications
    *
    * @return
    */
  val system: ActorSystem = ActorSystem(name, Cafienne.config.systemConfig) // Create an Akka system

  // Create singleton actors
  val timerService: ActorRef = system.actorOf(Props.create(classOf[TimerService], this), TimerService.CAFIENNE_TIMER_SERVICE);

  private val cafienneGateway = new CafienneGateway(this)


  /**
    * Retrieve a router for case messages. This will forward the messages to the correct case instance
    */
  lazy val gateway: CafienneGateway = cafienneGateway

  // First, start platform bootstrap configuration
  BootstrapPlatformConfiguration.run(this)
}

