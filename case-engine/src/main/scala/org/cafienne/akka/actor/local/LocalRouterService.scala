package org.cafienne.akka.actor.local

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import org.cafienne.akka.actor.MessageRouterService
import org.cafienne.cmmn.akka.command.CaseCommand
import org.cafienne.cmmn.instance.Case
import org.cafienne.processtask.akka.command.ProcessCommand
import org.cafienne.processtask.instance.ProcessTaskActor
import org.cafienne.tenant.TenantActor
import org.cafienne.tenant.akka.command.TenantCommand


/**
  * In-memory representation, router as singleton actor
  */
object LocalRouterService extends MessageRouterService {
  private var inMemoryRouter: Option[ActorRef] = None

  override def getCaseMessageRouter(): ActorRef = inMemoryRouter.get
  override def getProcessMessageRouter(): ActorRef = inMemoryRouter.get
  override def getTenantMessageRouter(): ActorRef = inMemoryRouter.get

  def apply = throw new IllegalArgumentException("Can only be created with an actorsystem as argument")

  def apply(system: ActorSystem) = {
    inMemoryRouter = Some(system.actorOf(Props(classOf[LocalRouter])))
    this
  }
}

/**
  * In-memory router for distributed AkkaCaseCommands across various case instances,
  * based on the case instance id. In a clustered setup this routing is done by the Sharding system,
  * but this in memory router is introduced to have the same logic without having to setup the sharding.
  * Mostly used within the {@link TestScript} framework
  */
class LocalRouter extends Actor with ActorLogging {
  val caseActors = collection.mutable.Map[String, ActorRef]()
  val processActors = collection.mutable.Map[String, ActorRef]()
  val tenantActors = collection.mutable.Map[String, ActorRef]()

  def getCaseActor(c: CaseCommand): ActorRef = {
    // Note: we have to make sure that a new case actor gets the correct akka path, by providing the case instance id as it's identifier
    caseActors getOrElseUpdate(c.actorId, context.system.actorOf(Props.create(classOf[Case]), c.actorId))
  }

  def getProcessActor(c: ProcessCommand): ActorRef = {
    // Note: we have to make sure that a new case actor gets the correct akka path, by providing the case instance id as it's identifier
    processActors getOrElseUpdate(c.actorId, context.system.actorOf(Props.create(classOf[ProcessTaskActor]), c.actorId))
  }

  def getTenantActor(c: TenantCommand): ActorRef = {
    // Note: we have to make sure that a new case actor gets the correct akka path, by providing the case instance id as it's identifier
    tenantActors getOrElseUpdate(c.actorId, context.system.actorOf(Props.create(classOf[TenantActor]), c.actorId))
  }

  def receive: Actor.Receive = {
    case c: CaseCommand => getCaseActor(c).tell(c, sender())
    case p: ProcessCommand => getProcessActor(p).tell(p, sender())
    case t: TenantCommand => getTenantActor(t).tell(t, sender())
    case other => log.info("The InMemoryRouter received an unknown command: " + other);
  }
}
