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
import org.cafienne.cmmn.akka.command._
import org.cafienne.cmmn.akka.command.team.{CaseTeam, CaseTeamMember, MemberKey}
import org.cafienne.infrastructure.akka.http.route.{CommandRoute, QueryRoute}
import org.cafienne.service.api.cases.CaseReader
import org.cafienne.service.api.model.{BackwardCompatibleTeamFormat, BackwardCompatibleTeamMemberFormat}
import org.cafienne.service.api.projection.CaseSearchFailure
import org.cafienne.service.api.projection.query.CaseQueries

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

  /**
    * Check existence of case, access of the platform user to that case, existence of the given team members
    * in the tenant of the case, and then create the command and send it to the case
    * @param platformUser
    * @param members
    * @param caseInstanceId
    * @param createCaseCommand
    * @return
    */
  def askCaseWithValidMembers(platformUser: PlatformUser, members: Seq[CaseTeamMember], caseInstanceId: String, createCaseCommand: CreateCaseCommand): Route = {
    validateCaseAccess(platformUser, caseInstanceId, tenantUser => {
      askCaseWithValidTeam(tenantUser, members, createCaseCommand)
    })
  }

  /**
    * Validate that the members exist in the tenant of the tenantUser, and, if so, create and send the command to the case
    * @param tenantUser
    * @param members
    * @param command
    * @return
    */
  def askCaseWithValidTeam(tenantUser: TenantUser, members: Seq[CaseTeamMember], command: CreateCaseCommand): Route = {
    val tenant = tenantUser.tenant
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
          askModelActor(command(tenantUser))
        }
      }
      case Failure(t: Throwable) => complete(StatusCodes.NotFound, t.getLocalizedMessage)
    }
  }

  /**
    * Check that the platform user is part of the case team for the given case instance id, and then
    * create the command and send it to the case
    * @param platformUser
    * @param caseInstanceId
    * @param createCaseCommand
    * @return
    */
  def askCase(platformUser: PlatformUser, caseInstanceId: String, createCaseCommand: CreateCaseCommand): Route = {
    validateCaseAccess(platformUser, caseInstanceId, tenantUser => {
      askModelActor(createCaseCommand.apply(tenantUser))
    })
  }

  /**
    * Validate that the platform user is part of the case team for the given case instance id, and if so,
    * invoke the sub route with the proper tenant user
    * @param platformUser
    * @param caseInstanceId
    * @param subRoute
    * @return
    */
  def validateCaseAccess(platformUser: PlatformUser, caseInstanceId: String, subRoute: TenantUser => Route): Route = {
    readLastModifiedHeader() { caseLastModified =>
      onComplete(handleSyncedQuery(() => caseQueries.authorizeCaseAccessAndReturnTenant(caseInstanceId, platformUser), caseLastModified)) {
        case Success(tenant) => subRoute(platformUser.getTenantUser(tenant))
        case Failure(error) => {
          error match {
            case t: CaseSearchFailure => complete(StatusCodes.NotFound, t.getLocalizedMessage)
            case _ => throw error
          }
        }
      }
    }
  }

  /**
    * Simple trait (functional interface) to create a case command given a tenant user
    */
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
