/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.akka.actor

import java.time.Instant
import java.util
import java.util.stream.Collectors

import akka.actor._
import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings, ShardRegion}
import com.typesafe.config.{Config, ConfigFactory}
import com.typesafe.scalalogging.LazyLogging
import org.cafienne.akka.actor.command.exception.MissingTenantException
import org.cafienne.akka.actor.identity.TenantUser
import org.cafienne.cmmn.akka.command.CaseCommand
import org.cafienne.cmmn.instance.Case
import org.cafienne.cmmn.instance.casefile.{JSONReader, ValueMap}
import org.cafienne.cmmn.repository.DefinitionProvider
import org.cafienne.processtask.akka.command.ProcessCommand
import org.cafienne.processtask.instance.ProcessTaskActor
import org.cafienne.tenant.TenantActor
import org.cafienne.tenant.akka.command.TenantCommand

/**
  *
  * A CaseSystem can be started either in Clustered mode, or as a Local system.
  * In the first case, it relies on Akka clustering and sharding to manage the case instances
  * and forward messages to the proper case instance.
  * In the local scenario, the case system is run in-memory, and messages are forwarded by
  * a simple in-memory router.
  */
object CaseSystem extends LazyLogging {

  final val caseShardTypeName: String = "case"
  final val processShardTypeName: String = "process"
  final val tenantShardTypeName: String = "tenant"
  val numberOfPartitions = 100
  val localSystemKey: Long = "localSystemKey".hashCode
  /**
    * Global startup moment of the whole case system. Is used by LastModifiedRegistration in the case service
    */
  val startupMoment = Instant.now
  val ActorIdlePeriod: Long = {
    var period: Long = 10 * 60; // Default to 10 minutes.
    if (CaseSystem.config.hasPath("actor.idle-period")) {
      period = CaseSystem.config.getLong("actor.idle-period");
      logger.info("Individual Case instances will be removed from Akka memory after they have been idle for " + period + " seconds")
    } else {
      logger.info("Using default of 10 minutes to remove idle Case instances from Akka memory")
    }
    period * 1000
  }

  if (! CaseSystem.config.hasPath("platform")) {
    throw new IllegalArgumentException("Check configuration property cafienne.platform. This must be available")
  }

  private val configuredDefaultTenant = if (CaseSystem.config.hasPath("platform.default-tenant")) {
    CaseSystem.config.getString("platform.default-tenant")
  } else {
    ""
  }

  def defaultTenant = {
    if (configuredDefaultTenant.isEmpty) {
      throw new MissingTenantException("Tenant property must have a value")
    }
    configuredDefaultTenant
  }

  val debugEnabled = if (CaseSystem.config.hasPath("debug")) {
    CaseSystem.config.getBoolean("debug")
  } else {
    false
  }

  val debugRouteOpenOption = "api.security.debug.events.open"
  /**
    * Returns true of the debug route is open (for developers using IDE to do debugging)
    */
  val developerRouteOpen = {
    if (CaseSystem.config.hasPath(debugRouteOpenOption)) {
      CaseSystem.config.getBoolean(debugRouteOpenOption)
    } else {
      false
    }
  }

  val platformOwners: util.List[String] = CaseSystem.config.getStringList("platform.owners")
  if (platformOwners.isEmpty) {
    throw new IllegalArgumentException("Platform owners cannot be an empty list. Check configuration property cafienne.platform.owners")
  }

  def isPlatformOwner(user: TenantUser) : Boolean = isPlatformOwner(user.id)

  def isPlatformOwner(userId: String) : Boolean = {
    // TTP: platformOwners should be taken as Set and "toLowerCase" initially, and then we can do "contains" instead
    logger.debug("Checking whether user "+userId+" is a platform owner; list of owners: "+platformOwners)
    platformOwners.stream().filter(o => o.equalsIgnoreCase(userId)).count() > 0
  }

  /**
    * DefinitionProvider provides an interface for loading metadata models
    */
  val DefinitionProvider: DefinitionProvider = loadDefinitionProvider
  val idExtractor: ShardRegion.ExtractEntityId = {
    case pl: CaseCommand =>
      val ret = (pl.actorId, pl)
      ret
    case pl: ProcessCommand =>
      val ret = (pl.actorId, pl)
      ret
    case pl: TenantCommand =>
      val ret = (pl.actorId, pl)
      ret
  }
  val shardResolver: ShardRegion.ExtractShardId = msg ⇒ msg match {
    case pl: CaseCommand =>
      val pidHashKey: Long = pl.actorId.hashCode()
      val shard = ((localSystemKey + pidHashKey) % numberOfPartitions).toString
      shard
    case pl: ProcessCommand =>
      val pidHashKey: Long = pl.actorId.hashCode()
      val shard = ((localSystemKey + pidHashKey) % numberOfPartitions).toString
      shard
    case pl: TenantCommand =>
      val pidHashKey: Long = pl.actorId.hashCode()
      val shard = ((localSystemKey + pidHashKey) % numberOfPartitions).toString
      shard
  }
  var messageRouterService: MessageRouterService = _

  /**
    * Returns the BuildInfo as a string (containing JSON)
    *
    * @return
    */
  def version: ValueMap = JSONReader.parse(org.cafienne.cmmn.akka.BuildInfo.toJson)

  /**
    * Start the case system based on Akka clustering
    */
  def startCluster()(implicit system: ActorSystem) {
    system.log.info("starting Case cluster-sharded system")

    // Start the shard system
    val caseShardSystem = ClusterSharding(system).start(
      typeName = CaseSystem.caseShardTypeName,
      entityProps = CaseSystem.props,
      settings = ClusterShardingSettings(system),
      extractEntityId = CaseSystem.idExtractor,
      extractShardId = CaseSystem.shardResolver)

    val processShardSystem = ClusterSharding(system).start(
      typeName = CaseSystem.processShardTypeName,
      entityProps = Props(classOf[ProcessTaskActor]),
      settings = ClusterShardingSettings(system),
      extractEntityId = CaseSystem.idExtractor,
      extractShardId = CaseSystem.shardResolver)

    val tenantShardSystem = ClusterSharding(system).start(
      typeName = CaseSystem.tenantShardTypeName,
      entityProps = Props(classOf[TenantActor]),
      settings = ClusterShardingSettings(system),
      extractEntityId = CaseSystem.idExtractor,
      extractShardId = CaseSystem.shardResolver)

    // Create the message router
    messageRouterService = ClusteredRouterService(system)
  }

  def props: Props = Props(classOf[Case])

  /**
    * Start an in-memory case system
    */
  def startLocal()(implicit system: ActorSystem) {
    // All we need is a message router
    messageRouterService = LocalRouterService(system)
  }

  /**
    * Retrieve a router for case messages. This will forward the messages to the correct case instance
    */
  def caseMessageRouter(): ActorRef = {
    messageRouterService.getCaseMessageRouter()
  }

  def tenantMessageRouter() : ActorRef = {
    messageRouterService.getTenantMessageRouter()
  }

  def processMessageRouter(): ActorRef = {
    messageRouterService.getProcessMessageRouter()
  }

  private def loadDefinitionProvider: DefinitionProvider = {
    val providerClassName = CaseSystem.config.getString("definitions.provider")
    Class.forName(providerClassName).newInstance().asInstanceOf[DefinitionProvider]
  }

  def config: Config = {
    val config = ConfigFactory.defaultApplication
    config.getConfig("cafienne")
  }

  /**
    * Returns the Open ID Connect configuration settings of this Case System
    */
  def OIDC: OIDCConfig = {
    val connectUrl = CaseSystem.config.getString("api.security.oidc.connect-url")
    val tokenUrl = CaseSystem.config.getString("api.security.oidc.token-url")
    val keysUrl = CaseSystem.config.getString("api.security.oidc.key-url")
    val authorizationUrl = CaseSystem.config.getString("api.security.oidc.authorization-url")
    val issuer = CaseSystem.config.getString("api.security.oidc.issuer")

    OIDCConfig(connectUrl, tokenUrl, keysUrl, authorizationUrl, issuer)
  }
}

