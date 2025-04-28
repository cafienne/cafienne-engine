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

package org.cafienne.system.router

import org.apache.pekko.actor.{Actor, ActorRef, ActorSystem, Props}
import org.apache.pekko.cluster.sharding.{ClusterSharding, ClusterShardingSettings, ShardRegion}
import org.apache.pekko.util.Timeout
import org.cafienne.actormodel.command.ModelCommand
import org.cafienne.cmmn.actorapi.command.CaseCommand
import org.cafienne.cmmn.instance.Case
import org.cafienne.consentgroup.ConsentGroupActor
import org.cafienne.consentgroup.actorapi.command.ConsentGroupCommand
import org.cafienne.processtask.actorapi.command.ProcessCommand
import org.cafienne.processtask.instance.ProcessTaskActor
import org.cafienne.storage.StorageCoordinator
import org.cafienne.storage.actormodel.command.StorageCommand
import org.cafienne.system.CaseSystem
import org.cafienne.tenant.TenantActor
import org.cafienne.tenant.actorapi.command.TenantCommand

import scala.concurrent.Future

class ClusteredCaseEngineGateway(caseSystem: CaseSystem) extends GatewayMessageRouter {
  val system: ActorSystem = caseSystem.system
  final val caseShardTypeName: String = "case"
  final val processShardTypeName: String = "process"
  final val tenantShardTypeName: String = "tenant"
  final val consentGroupShardTypeName: String = "consentgroup"
  final val storageShardTypeName: String = "storage"

  //pekko.cluster.sharding.number-of-shards
  private val numberOfPartitions = system.settings.config.getInt("pekko.cluster.sharding.number-of-shards")

  private val idExtractor: ShardRegion.ExtractEntityId = {
    case command: CaseCommand => (command.actorId, command)
    case command: ProcessCommand => (command.actorId, command)
    case command: TenantCommand => (command.actorId, command)
    case command: ConsentGroupCommand => (command.actorId, command)
    case command: StorageCommand => (command.metadata.actorId, command)
    case other => throw new Error(s"Cannot extract actor id for messages of type ${other.getClass.getName}")
  }

  //Subcases, processtasks need to end up at the same shard as the original case. (also in follow up calls)
  //use the rootCaseId for deciding on the shard. This is added to the CaseMembership
  private val shardResolver: ShardRegion.ExtractShardId = {
    case m: ModelCommand => {
      val pidHashKey: Long = m.getRootCaseId.hashCode
      val shard = (pidHashKey % numberOfPartitions).toString
      shard
    }
    case s: StorageCommand =>
      //TODO storage need to be handled via rootCaseId
      val pidHashKey: Long = s.metadata.actorId.hashCode
      val shard =  (pidHashKey % numberOfPartitions).toString
      shard
    case ShardRegion.StartEntity(id) =>
      // StartEntity is used by remembering entities feature
      (id.toLong % numberOfPartitions).toString
    case other => {
      System.err.println(s"\nShard resolver for messages of type ${other.getClass.getName} is not supported")
      // Unsupported command type
      val shard = (other.hashCode() % numberOfPartitions).toString
      shard
    }
  }

  private def startShard(typeName: String, clazz: Class[_]) = {
    ClusterSharding(caseSystem.system)
      .start(typeName = typeName,
        entityProps = Props(clazz, caseSystem),
        settings = ClusterShardingSettings(caseSystem.system),
        extractEntityId = idExtractor,
        extractShardId = shardResolver)
  }

  // Start the shard system
  private val clusteredCaseRegion = startShard(caseShardTypeName, classOf[Case])
  private val clusteredProcessTaskRegion = startShard(processShardTypeName, classOf[ProcessTaskActor])
  private val clusteredTenantRegion = startShard(tenantShardTypeName, classOf[TenantActor])
  private val clusteredConsentGroupRegion = startShard(consentGroupShardTypeName, classOf[ConsentGroupActor])
  private val clusteredStorageRegion = startShard(storageShardTypeName, classOf[StorageCoordinator])

  def request(message: Any): Future[Any] = {
    import org.apache.pekko.pattern.ask
    implicit val timeout: Timeout = caseSystem.config.actor.askTimout

    getRouter(message).map(actorRef => actorRef.ask(message)).getOrElse(Future.failed(new Exception(s"No router found for message $message")))
  }

  def inform(message: Any, sender: ActorRef = Actor.noSender): Unit = {
    getRouter(message).map(actorRef => actorRef.tell(message, sender)).getOrElse(system.log.error(s"No router found for message $message"))
  }

  private def getRouter(message: Any): Option[ActorRef] = {
    message match {
      case _: StorageCommand => Some(clusteredStorageRegion)
      case command: ModelCommand =>
        val actorClass = command.actorClass()
        if (actorClass == classOf[Case]) return Some(clusteredCaseRegion)
        if (actorClass == classOf[ProcessTaskActor]) return Some(clusteredProcessTaskRegion)
        if (actorClass == classOf[TenantActor]) return Some(clusteredTenantRegion)
        if (actorClass == classOf[ConsentGroupActor]) return Some(clusteredConsentGroupRegion)
        None
      case _ => None
    }
  }
}
