package org.cafienne.akka.actor.cluster

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings, ShardRegion}
import org.cafienne.akka.actor.MessageRouterService
import org.cafienne.cmmn.akka.command.CaseCommand
import org.cafienne.cmmn.instance.Case
import org.cafienne.processtask.akka.command.ProcessCommand
import org.cafienne.processtask.instance.ProcessTaskActor
import org.cafienne.tenant.TenantActor
import org.cafienne.tenant.akka.command.TenantCommand

/**
  * Clustered representation, router as singleton actor
  */
object ClusteredRouterService extends MessageRouterService {
  private var caseShardRouter: Option[ActorRef] = None
  private var processShardRouter: Option[ActorRef] = None
  private var tenantShardRouter: Option[ActorRef] = None

  override def getCaseMessageRouter(): ActorRef = caseShardRouter.get

  override def getProcessMessageRouter(): ActorRef = processShardRouter.get

  override def getTenantMessageRouter(): ActorRef = tenantShardRouter.get

  def apply = throw new IllegalArgumentException("Can only be created with an actorsystem as argument")

  final val caseShardTypeName: String = "case"
  final val processShardTypeName: String = "process"
  final val tenantShardTypeName: String = "tenant"
  val numberOfPartitions = 100
  val localSystemKey: Long = "localSystemKey".hashCode


  def apply(system: ActorSystem) = {

    // Start the shard system
    val caseShardSystem = ClusterSharding(system).start(
      typeName = caseShardTypeName,
      entityProps = Props(classOf[Case]),
      settings = ClusterShardingSettings(system),
      extractEntityId = idExtractor,
      extractShardId = shardResolver)

    val processShardSystem = ClusterSharding(system).start(
      typeName = processShardTypeName,
      entityProps = Props(classOf[ProcessTaskActor]),
      settings = ClusterShardingSettings(system),
      extractEntityId = idExtractor,
      extractShardId = shardResolver)

    val tenantShardSystem = ClusterSharding(system).start(
      typeName = tenantShardTypeName,
      entityProps = Props(classOf[TenantActor]),
      settings = ClusterShardingSettings(system),
      extractEntityId = idExtractor,
      extractShardId = shardResolver)

    caseShardRouter = Some(ClusterSharding(system).shardRegion(caseShardTypeName))
    processShardRouter = Some(ClusterSharding(system).shardRegion(processShardTypeName))
    tenantShardRouter = Some(ClusterSharding(system).shardRegion(tenantShardTypeName))
    this
  }


  private val idExtractor: ShardRegion.ExtractEntityId = {
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

  private val shardResolver: ShardRegion.ExtractShardId = msg ⇒ msg match {
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
}
