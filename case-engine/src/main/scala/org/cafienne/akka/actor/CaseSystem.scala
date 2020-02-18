/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.akka.actor

import java.time.Instant

import akka.actor._
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.LazyLogging
import org.cafienne.akka.actor.config.CafienneConfig
import org.cafienne.akka.actor.identity.TenantUser
import org.cafienne.akka.actor.router.{ClusterRouter, LocalRouter}
import org.cafienne.cmmn.instance.casefile.{JSONReader, ValueMap}

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
    * Configuration settings of this Cafienne Platform
    */
  val config = {
    val fallback = ConfigFactory.defaultReference()
    val config = ConfigFactory.load().withFallback(fallback)
    new CafienneConfig(config)
  }

  def isPlatformOwner(user: TenantUser): Boolean = isPlatformOwner(user.id)

  def isPlatformOwner(userId: String): Boolean = {
    config.platform.isPlatformOwner(userId)
  }

  /**
    * Returns the BuildInfo as a string (containing JSON)
    *
    * @return
    */
  def version: ValueMap = JSONReader.parse(org.cafienne.cmmn.akka.BuildInfo.toJson)

  /**
    * Start the Case System. This will spin up an akka system according to the specifications
    * @return
    */
  def start(name: String = "Cafienne-Case-System") = {

    // Create an Akka system
    system = ActorSystem(name)

    val routerClazz = system.hasExtension(akka.cluster.Cluster) match{
      case true => classOf[ClusterRouter]
      case false => classOf[LocalRouter]
    }
    messageRouterService = system.actorOf(Props.create(routerClazz));
  }

  var messageRouterService: ActorRef = _
  var system: ActorSystem = null

  /**
    * Retrieve a router for case messages. This will forward the messages to the correct case instance
    */
  def router(): ActorRef = {
    messageRouterService
  }
}

