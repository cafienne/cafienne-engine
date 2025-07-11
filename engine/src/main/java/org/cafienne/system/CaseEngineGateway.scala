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

package org.cafienne.system

import org.apache.pekko.actor.{Actor, ActorRef, ActorSystem, Props}
import org.apache.pekko.util.Timeout
import org.cafienne.actormodel.command.{ModelCommand, TerminateModelActor}
import org.cafienne.actormodel.response.{ActorTerminated, ModelResponse}
import org.cafienne.engine.cmmn.instance.Case
import org.cafienne.consentgroup.ConsentGroupActor
import org.cafienne.engine.processtask.instance.ProcessTaskActor
import org.cafienne.system.router.LocalRouter
import org.cafienne.tenant.TenantActor

import scala.concurrent.Future

class CaseEngineGateway(caseSystem: CaseSystem) {
  private val system: ActorSystem = caseSystem.system
  private val terminationRequests = collection.concurrent.TrieMap[String, ActorRef]()
  private val actors = collection.concurrent.TrieMap[String, ActorRef]()
  private val caseService = system.actorOf(Props.create(classOf[LocalRouter], caseSystem, actors, terminationRequests), "cases")
  private val processTaskService = system.actorOf(Props.create(classOf[LocalRouter], caseSystem, actors, terminationRequests), "process-tasks")
  private val tenantService = system.actorOf(Props.create(classOf[LocalRouter], caseSystem, actors, terminationRequests), "tenants")
  private val consentGroupService = system.actorOf(Props.create(classOf[LocalRouter], caseSystem, actors, terminationRequests), "consent-groups")
  private val defaultRouterService: ActorRef = system.actorOf(Props.create(classOf[LocalRouter], caseSystem, actors, terminationRequests), "default-router")

  def request(message: ModelCommand): Future[ModelResponse] = {
    import org.apache.pekko.pattern.ask
    implicit val timeout: Timeout = caseSystem.config.actor.askTimout

    getRouter(message).ask(message).asInstanceOf[Future[ModelResponse]]
  }

  def inform(message: ModelCommand, sender: ActorRef = Actor.noSender): Unit = {
    getRouter(message).tell(message, sender)
  }

  def terminate(actorId: String): Unit = {
    defaultRouterService.tell(TerminateModelActor(actorId), ActorRef.noSender)
  }

  def awaitTermination(actorId: String): Future[ActorTerminated] = {
    import org.apache.pekko.pattern.ask
    implicit val timeout: Timeout = caseSystem.config.actor.askTimout

    defaultRouterService.ask(TerminateModelActor(actorId)).asInstanceOf[Future[ActorTerminated]]
  }

  private def getRouter(message: ModelCommand): ActorRef = {
    message match {
      case command: ModelCommand =>
        val actorClass = command.actorClass()
        // Unfortunately for some reason we cannot use scala matching on the actor class.
        // Unclear why (most probably lack of scala knowledge ;))
        if (actorClass == classOf[Case]) return caseService
        if (actorClass == classOf[ProcessTaskActor]) return processTaskService
        if (actorClass == classOf[TenantActor]) return tenantService
        if (actorClass == classOf[ConsentGroupActor]) return consentGroupService
        defaultRouterService
      case _ => defaultRouterService
    }
  }
}
