/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.service.api.cases.route

import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives.{complete, _}
import akka.http.scaladsl.server.Route
import org.cafienne.akka.actor.identity.{PlatformUser, TenantUser}
import org.cafienne.cmmn.actorapi.command._
import org.cafienne.cmmn.actorapi.command.team.{CaseTeam, CaseTeamMember, MemberKey}
import org.cafienne.infrastructure.akka.http.route.{CommandRoute, QueryRoute}
import org.cafienne.service.api.cases.CaseReader
import org.cafienne.service.api.model.{BackwardCompatibleTeamFormat, BackwardCompatibleTeamMemberFormat}
import org.cafienne.service.db.query.CaseQueries
import org.cafienne.service.db.query.exception.CaseSearchFailure

import scala.util.{Failure, Success}

trait CasesRoute extends CommandRoute with QueryRoute {
  val caseQueries: CaseQueries

  override val lastModifiedRegistration = CaseReader.lastModifiedRegistration

  /**
    * Run the sub route with a valid platform user and case instance id
    * @param subRoute
    * @return
    */
  def caseInstanceRoute(subRoute: (PlatformUser, String) => Route) : Route = {
    validUser { platformUser =>
      path(Segment) { caseInstanceId =>
        pathEndOrSingleSlash {
          subRoute(platformUser, caseInstanceId)
        }
      }
    }
  }

  /**
    * Run the sub route with a valid platform user and case instance id
    * @param subRoute
    * @return
    */
  def caseInstanceSubRoute(subRoute: (PlatformUser, String) => Route) : Route = {
    validUser { platformUser =>
      pathPrefix(Segment) { caseInstanceId =>
        subRoute(platformUser, caseInstanceId)
      }
    }
  }

  /**
    * Run the sub route with a valid platform user and case instance id
    * @param subRoute
    * @return
    */
  def caseInstanceSubRoute(prefix: String, subRoute: (PlatformUser, String) => Route) : Route = {
    validUser { platformUser =>
      pathPrefix(Segment / prefix) { caseInstanceId =>
        subRoute(platformUser, caseInstanceId)
      }
    }
  }

  def askCaseWithValidMembers(platformUser: PlatformUser, members: Seq[CaseTeamMember], caseInstanceId: String, createCaseCommand: CreateCaseCommand): Route = {
    readLastModifiedHeader() { caseLastModified =>
      onComplete(handleSyncedQuery(() => caseQueries.authorizeCaseAccessAndReturnTenant(caseInstanceId, platformUser), caseLastModified)) {
        case Success(tenant) => {
          askCaseWithValidTeam(tenant, members, createCaseCommand.apply(platformUser.getTenantUser(tenant)))
        }
        case Failure(error) => {
          error match {
            case t: CaseSearchFailure => complete(StatusCodes.NotFound, t.getLocalizedMessage)
            case _ => throw error
          }
        }
      }
    }
  }

  def askCaseWithValidTeam(tenant: String, members: Seq[CaseTeamMember], command: CaseCommand): Route = {
    val userIds = members.filter(member => member.isTenantUser()).map(member => member.key.id)
    onComplete(userCache.getUsers(userIds, tenant)) {
      case Success(tenantUsers) => {
        if (tenantUsers.size != userIds.size) {
          val tenantUserIds = tenantUsers.map(t => t.id)
          val unfoundUsers = userIds.filterNot(userId => tenantUserIds.contains(userId))
          val msg = {
            if (unfoundUsers.size == 1) s"Cannot find an active user '${unfoundUsers(0)}' in tenant '$tenant'"
            else s"The users ${unfoundUsers.map(u => s"'$u'").mkString(", ")} are not active in tenant $tenant"
          }
          complete(StatusCodes.NotFound, msg)
        } else {
          askModelActor(command)
        }
      }
      case Failure(t: Throwable) => complete(StatusCodes.NotFound, t.getLocalizedMessage)
    }
  }

  def askCase(platformUser: PlatformUser, caseInstanceId: String, createCaseCommand: CreateCaseCommand): Route = {
    readLastModifiedHeader() { caseLastModified =>
      onComplete(handleSyncedQuery(() => caseQueries.authorizeCaseAccessAndReturnTenant(caseInstanceId, platformUser), caseLastModified)) {
        case Success(tenant) => askModelActor(createCaseCommand.apply(platformUser.getTenantUser(tenant)))
        case Failure(error) => {
          error match {
            case t: CaseSearchFailure => complete(StatusCodes.NotFound, t.getLocalizedMessage)
            case _ => throw error
          }
        }
      }
    }
  }

  trait CreateCaseCommand {
    def apply(user: TenantUser): CaseCommand
  }

  protected def teamConverter(caseTeam: BackwardCompatibleTeamFormat): CaseTeam = {
    CaseTeam(caseTeam.members.map {
      memberConverter
    })
  }

  protected def memberConverter(member: BackwardCompatibleTeamMemberFormat): CaseTeamMember = {
    val memberId = member.memberId.getOrElse(member.user.getOrElse(throw new IllegalArgumentException("Member id is missing")))
    val memberType = member.memberType.getOrElse("user")
    val caseRoles = member.caseRoles.getOrElse(member.roles.getOrElse(Seq()))
    val removeRoles = member.removeRoles.getOrElse(Seq())

    val isOwner = {
      if (member.isOwner.nonEmpty) member.isOwner // If the value of owner is filled, then that precedes (both in old and new format)
      else if (member.user.nonEmpty) Some(true) // Old format ==> all users become owner
      else member.isOwner // New format, take what is set
    }

    new CaseTeamMember(MemberKey(memberId, memberType), caseRoles = caseRoles, isOwner = isOwner, removeRoles = removeRoles)
  }
}
