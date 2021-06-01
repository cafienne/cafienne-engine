/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.akka.actor

import akka.actor._
import com.typesafe.scalalogging.LazyLogging
import org.cafienne.akka.actor.config.Cafienne
import org.cafienne.akka.actor.health.HealthMonitor
import org.cafienne.akka.actor.identity.TenantUser
import org.cafienne.akka.actor.router.{ClusterRouter, LocalRouter}
import org.cafienne.platform.PlatformService
import org.cafienne.timerservice.TimerService

import java.time.Instant

/**
  *
  * A CaseSystem can be started either in Clustered mode, or as a Local system.
  * In the first case, it relies on Akka clustering and sharding to manage the case instances
  * and forward messages to the proper case instance.
  * In the local scenario, the case system is run in-memory, and messages are forwarded by
  * a simple in-memory router.
  */
object CaseSystem extends LazyLogging {

  /**
    * Global startup moment of the whole case system. Is used by LastModifiedRegistration in the case service
    */
  val startupMoment = Instant.now

  /**
    * Health monitor has latest status information on health of the Case System
    */
  val health = new HealthMonitor
  var messageRouterService: ActorRef = _
  var platformService: ActorRef = _
  var timerService: ActorRef = _
  var system: ActorSystem = null

  def isPlatformOwner(user: TenantUser): Boolean = isPlatformOwner(user.id)

  def isPlatformOwner(userId: String): Boolean = {
    Cafienne.config.platform.isPlatformOwner(userId)
  }

  /**
    * Start the Case System. This will spin up an akka system according to the specifications
    *
    * @return
    */
  def start(name: String = "Cafienne-Case-System") = {
    // Create an Akka system
    system = ActorSystem(name)

    val routerClazz = system.hasExtension(akka.cluster.Cluster) match {
      case true => classOf[ClusterRouter]
      case false => classOf[LocalRouter]
    }

    // Always immediately create a TimerService
    platformService = system.actorOf(Props.create(classOf[PlatformService]), PlatformService.CAFIENNE_PLATFORM_SERVICE);
    timerService = system.actorOf(Props.create(classOf[TimerService]), TimerService.CAFIENNE_TIMER_SERVICE);

    messageRouterService = system.actorOf(Props.create(routerClazz))
  }

  /**
    * Retrieve a router for case messages. This will forward the messages to the correct case instance
    */
  def router(): ActorRef = {
    messageRouterService
  }
}

