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
import org.apache.pekko.util.Timeout
import org.cafienne.cmmn.actorapi.command.CaseCommand
import org.cafienne.consentgroup.actorapi.command.ConsentGroupCommand
import org.cafienne.infrastructure.Cafienne
import org.cafienne.processtask.actorapi.command.ProcessCommand
import org.cafienne.system.CaseSystem
import org.cafienne.tenant.actorapi.command.TenantCommand

import scala.concurrent.Future

class CafienneGateway(caseSystem: CaseSystem) {
  private val system: ActorSystem = caseSystem.system
  private val terminationRequests = collection.concurrent.TrieMap[String, ActorRef]()
  private val actors = collection.concurrent.TrieMap[String, ActorRef]()
  private val caseService = system.actorOf(Props.create(classOf[LocalRouter], caseSystem, actors, terminationRequests), "cases")
  private val processTaskService = system.actorOf(Props.create(classOf[LocalRouter], caseSystem, actors, terminationRequests), "process-tasks")
  private val tenantService = system.actorOf(Props.create(classOf[LocalRouter], caseSystem, actors, terminationRequests), "tenants")
  private val consentGroupService = system.actorOf(Props.create(classOf[LocalRouter], caseSystem, actors, terminationRequests), "consent-groups")
  private val defaultRouterService: ActorRef = system.actorOf(Props.create(classOf[LocalRouter], caseSystem, actors, terminationRequests), "default-router")

  def request(message: Any): Future[Any] = {
    import org.apache.pekko.pattern.ask
    implicit val timeout: Timeout = Cafienne.config.actor.askTimout

    getRouter(message).ask(message)
  }

  def inform(message: Any, sender: ActorRef = Actor.noSender): Unit = {
    getRouter(message).tell(message, sender)
  }

  private def getRouter(message: Any): ActorRef = {
    message match {
      case _: CaseCommand => caseService
      case _: ProcessCommand => processTaskService
      case _: TenantCommand => tenantService
      case _: ConsentGroupCommand => consentGroupService
      case _ => defaultRouterService
    }
  }
}
