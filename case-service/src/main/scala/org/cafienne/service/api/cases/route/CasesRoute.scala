/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.service.api.cases.route

import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import org.cafienne.akka.actor.identity.{PlatformUser, TenantUser}
import org.cafienne.cmmn.akka.command._
import org.cafienne.cmmn.akka.command.team.{CaseTeam, CaseTeamMember, MemberKey}
import org.cafienne.infrastructure.akka.http.route.CommandRoute
import org.cafienne.service.api
import org.cafienne.service.api.cases.{CaseQueries, CaseReader}
import org.cafienne.service.api.model.{BackwardCompatibleTeam, BackwardCompatibleTeamMember}
import org.cafienne.service.api.projection.CaseSearchFailure

import scala.util.{Failure, Success}

trait CasesRoute extends CommandRoute with CaseReader {
  val caseQueries: CaseQueries

  def askCase(platformUser: PlatformUser, caseInstanceId: String, createCaseCommand: CreateCaseCommand): Route = {
    optionalHeaderValueByName(api.CASE_LAST_MODIFIED) { caseLastModified =>
      onComplete(handleSyncedQuery(() => caseQueries.authorizeCaseAccess(caseInstanceId, platformUser), caseLastModified)) {
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

  protected def teamConverter(caseTeam: BackwardCompatibleTeam): CaseTeam = {
    if (caseTeam == null) CaseTeam()
    else new CaseTeam(caseTeam.members.map{memberConverter})
  }

  protected def memberConverter(member: BackwardCompatibleTeamMember): CaseTeamMember = {
    val mId = member.memberId.getOrElse(member.user.getOrElse(throw new IllegalArgumentException("Member id is missing")))
    val cr = member.caseRoles.getOrElse(member.roles.getOrElse(Seq()))
    new CaseTeamMember(MemberKey(mId, member.memberType.getOrElse("user")), caseRoles = cr, isOwner = member.isOwner, removeRoles = member.removeRoles.getOrElse(Seq()))
  }
}
