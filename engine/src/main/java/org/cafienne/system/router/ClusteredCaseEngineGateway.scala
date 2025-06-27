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

import org.apache.pekko.actor.{Actor, ActorRef, ActorSystem, PoisonPill, Props, Terminated}
import org.apache.pekko.cluster.sharding.{ClusterSharding, ClusterShardingSettings, ShardRegion}
import org.apache.pekko.cluster.singleton.{ClusterSingletonManager, ClusterSingletonManagerSettings, ClusterSingletonProxy, ClusterSingletonProxySettings}
import org.apache.pekko.util.Timeout
import org.cafienne.actormodel.command.{ModelCommand, TerminateModelActor}
import org.cafienne.actormodel.communication.CaseSystemCommunicationCommand
import org.cafienne.cmmn.actorapi.command.CaseCommand
import org.cafienne.cmmn.instance.Case
import org.cafienne.consentgroup.ConsentGroupActor
import org.cafienne.consentgroup.actorapi.command.ConsentGroupCommand
import org.cafienne.processtask.actorapi.command.ProcessCommand
import org.cafienne.processtask.instance.ProcessTaskActor
import org.cafienne.storage.StorageCoordinator
import org.cafienne.storage.actormodel.command.{ClearTimerData, StorageCommand}
import org.cafienne.system.CaseSystem
import org.cafienne.tenant.TenantActor
import org.cafienne.tenant.actorapi.command.TenantCommand
import org.cafienne.timerservice.TimerService

import scala.annotation.tailrec
import scala.concurrent.Future

class ClusteredCaseEngineGateway(caseSystem: CaseSystem) extends GatewayMessageRouter {
  val system: ActorSystem = caseSystem.system
  final val caseShardTypeName: String = "case"
  final val processShardTypeName: String = "process"
  final val tenantShardTypeName: String = "tenant"
  final val consentGroupShardTypeName: String = "consentgroup"

  //pekko.cluster.sharding.number-of-shards
  private val numberOfPartitions = system.settings.config.getInt("pekko.cluster.sharding.number-of-shards")

  private val idExtractor: ShardRegion.ExtractEntityId = {
    case command: CaseCommand => (command.actorId, command)
    case command: ProcessCommand => (command.actorId, command)
    case command: TenantCommand => (command.actorId, command)
    case command: ConsentGroupCommand => (command.actorId, command)
    case command: StorageCommand => (command.metadata.actorId, command)
    case command: CaseSystemCommunicationCommand => (command.actorId, command)
    case msg: TerminateModelActor => (msg.actorId, (msg))//ShardRegion.Passivate(PoisonPill) (or TerminateModelActor itself)
    case startEntity: ShardRegion.StartEntity => (startEntity.entityId, startEntity)
    case other => throw new Error(s"Cannot extract actor id for messages of type ${other.getClass.getName}")
  }

  //Subcases, processtasks need to end up at the same shard as the original case. (also in follow up calls)
  //use the rootCaseId for deciding on the shard. This is added to the CaseMembership
  private val shardResolver: ShardRegion.ExtractShardId = {
    case ShardRegion.StartEntity(id) =>
      // StartEntity is used by remembering entities feature
      (id.hashCode.toLong % numberOfPartitions).toString
    case TerminateModelActor(id, clazz) =>
      system.log.info(s"TerminateModelActor ${clazz.getSimpleName} message received for id: " + id)
      (id.hashCode.toLong % numberOfPartitions).toString
    case Terminated(actorRef) =>
      system.log.info("Terminated message received for actorRef: " + actorRef + " not routed via root case id")
      (actorRef.hashCode() % numberOfPartitions).toString
    case c: CaseSystemCommunicationCommand =>
      val pidHashKey: Long = c.command.getRootCaseId.hashCode
      (pidHashKey % numberOfPartitions).toString
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
    case other => {
      system.log.warning(s"\nShard resolver for messages of type ${other.getClass.getName} is not supported")
      // Unsupported command type
      val shard = (other.hashCode() % numberOfPartitions).toString
      shard
    }
  }

  private def startShardingNode(typeName: String, clazz: Class[_]) = {
    ClusterSharding(caseSystem.system)
      .start(typeName = typeName,
        entityProps = Props(clazz, caseSystem),
        settings = ClusterShardingSettings(caseSystem.system),
        extractEntityId = idExtractor,
        extractShardId = shardResolver)
  }

  // Start the shard system
  private val clusteredCaseRegion = startShardingNode(caseShardTypeName, classOf[Case])
  private val clusteredProcessTaskRegion = startShardingNode(processShardTypeName, classOf[ProcessTaskActor])
  private val clusteredTenantRegion = startShardingNode(tenantShardTypeName, classOf[TenantActor])
  private val clusteredConsentGroupRegion = startShardingNode(consentGroupShardTypeName, classOf[ConsentGroupActor])

  def request(message: Any): Future[Any] = {
    import org.apache.pekko.pattern.ask
    implicit val timeout: Timeout = caseSystem.config.actor.askTimout

    getRouter(message).map(actorRef => actorRef.ask(message)).getOrElse(Future.failed(new Exception(s"Cluster: No router found for message $message")))
  }

  def inform(message: Any, sender: ActorRef = Actor.noSender): Unit = {
    getRouter(message).map(actorRef => actorRef.tell(message, sender)).getOrElse(system.log.error(s"Cluster: No router found for message $message"))
  }

  @tailrec
  private def getRouter(message: Any): Option[ActorRef] = {
    message match {
      case _: StorageCommand => Some(storageCoordinator)
      case _: ClearTimerData => Some(timerService)
      case modelActor: TerminateModelActor =>
        if (modelActor.clazz equals(classOf[TenantActor])) {
          Some(clusteredTenantRegion)
        } else {
          Some(clusteredCaseRegion)
        }
      case cmd: CaseSystemCommunicationCommand => getRouter(cmd.command)
      case _: Terminated => Some(clusteredCaseRegion)
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

  private def createSingleton(actorClass: Class[_], name: String): ActorRef = {
    system.actorOf(
      ClusterSingletonManager.props(
        singletonProps = Props(actorClass, this.caseSystem),
        terminationMessage = PoisonPill,
        settings = ClusterSingletonManagerSettings(system)),
      name = s"$name")
    //return access to the singleton actor
    system.actorOf(
      ClusterSingletonProxy.props(
        singletonManagerPath = s"/user/$name",
        settings = ClusterSingletonProxySettings(system)),
      name = s"$name-proxy")
  }

  private val timerService: ActorRef = createSingleton(classOf[TimerService], TimerService.IDENTIFIER)
  private val storageCoordinator: ActorRef = createSingleton(classOf[StorageCoordinator], StorageCoordinator.IDENTIFIER)
}
