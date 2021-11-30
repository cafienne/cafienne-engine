package org.cafienne.system.router

import akka.actor.{ActorRef, Props}
import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings, ShardRegion}
import org.cafienne.actormodel.command.ModelCommand
import org.cafienne.cmmn.actorapi.command.CaseCommand
import org.cafienne.cmmn.instance.Case
import org.cafienne.consentgroup.ConsentGroupActor
import org.cafienne.consentgroup.actorapi.command.ConsentGroupCommand
import org.cafienne.processtask.actorapi.command.ProcessCommand
import org.cafienne.processtask.instance.ProcessTaskActor
import org.cafienne.system.CaseSystem
import org.cafienne.tenant.TenantActor
import org.cafienne.tenant.actorapi.command.TenantCommand

/**
  * Clustered representation, router as singleton actor
  */
class ClusterRouter(val caseSystem: CaseSystem) extends CaseMessageRouter {
  logger.info("Starting case system in cluster mode")

  final val caseShardTypeName: String = "case"
  final val processShardTypeName: String = "process"
  final val tenantShardTypeName: String = "tenant"
  final val consentGroupShardTypeName: String = "consentgroup"
  private lazy val caseShardRouter: ActorRef = ClusterSharding(context.system).shardRegion(caseShardTypeName)
  private lazy val processShardRouter: ActorRef = ClusterSharding(context.system).shardRegion(processShardTypeName)
  private lazy val tenantShardRouter: ActorRef = ClusterSharding(context.system).shardRegion(tenantShardTypeName)
  private lazy val consentGroupShardRouter: ActorRef = ClusterSharding(context.system).shardRegion(consentGroupShardTypeName)
  val numberOfPartitions = 100
  val localSystemKey: Long = "localSystemKey".hashCode
  private val idExtractor: ShardRegion.ExtractEntityId = {
    case command: CaseCommand => (command.actorId, command)
    case command: ProcessCommand => (command.actorId, command)
    case command: TenantCommand => (command.actorId, command)
    case command: ConsentGroupCommand => (command.actorId, command)
    case other => throw new Error(s"Cannot extract actor id for messages of type ${other.getClass.getName}")
  }
  private val shardResolver: ShardRegion.ExtractShardId = {
    case m: ModelCommand[_, _] => {
      val pidHashKey: Long = m.actorId.hashCode()
      val shard = ((localSystemKey + pidHashKey) % numberOfPartitions).toString
      shard
    }
    case other => {
      System.err.println(s"\nShard resolver for messages of type ${other.getClass.getName} is not supported")
      // Unsupported command type
      val shard = ((localSystemKey + other.hashCode()) % numberOfPartitions).toString
      shard
    }
  }

  override def forwardMessage(m: ModelCommand[_, _]) = {
    // Forward message into the right shardregion
    val shardRouter: ActorRef = m match {
      case _: CaseCommand => caseShardRouter
      case _: ProcessCommand => processShardRouter
      case _: TenantCommand => tenantShardRouter
      case _: ConsentGroupCommand => consentGroupShardRouter
      case other => throw new Error(s"Cannot forward ModelCommands of type ${other.getClass.getName}")
    }
    shardRouter.forward(m)
  }

  /**
    * Create the shard system first before we can handle messages
    */
  override def preStart(): Unit = {

    // Note: we're creating 3 shards, one for each type of ModelActor currently known.
    //  This is needed because the classic sharding system that is used here does not
    //  support different/dynamic types of Props in the entityProps.
    //  Perhaps with the new akka 2.6 this can be solved in a new way currently not known to us.

    def startShard(typeName: String, clazz: Class[_]) = {
      ClusterSharding(context.system).start(typeName = typeName, entityProps = Props(clazz, caseSystem), settings = ClusterShardingSettings(context.system), extractEntityId = idExtractor, extractShardId = shardResolver)
    }

    // Start the shard system
    startShard(caseShardTypeName, classOf[Case])
    startShard(processShardTypeName, classOf[ProcessTaskActor])
    startShard(tenantShardTypeName, classOf[TenantActor])
    startShard(consentGroupShardTypeName, classOf[ConsentGroupActor])
  }

  override def terminateActor(actorId: String): Unit = {
    System.err.println("\nTerminating actors is not implemented in clustered environments")
  }
}
