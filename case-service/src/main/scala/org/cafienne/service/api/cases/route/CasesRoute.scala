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
import org.cafienne.infrastructure.akka.http.route.{CommandRoute, QueryRoute}
import org.cafienne.service.api
import org.cafienne.service.api.cases.CaseReader
import org.cafienne.service.api.model.{BackwardCompatibleTeam, BackwardCompatibleTeamMember}
import org.cafienne.service.api.projection.CaseSearchFailure
import org.cafienne.service.api.projection.query.CaseQueries

import scala.util.{Failure, Success}

trait CasesRoute extends CommandRoute with QueryRoute {
  val caseQueries: CaseQueries

  override val lastModifiedRegistration = CaseReader.lastModifiedRegistration

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

  protected def teamConverter(caseTeam: BackwardCompatibleTeam): CaseTeam = {
    CaseTeam(caseTeam.members.map {
      memberConverter
    })
  }

  protected def memberConverter(member: BackwardCompatibleTeamMember): CaseTeamMember = {
    val mId = member.memberId.getOrElse(member.user.getOrElse(throw new IllegalArgumentException("Member id is missing")))
    val cr = member.caseRoles.getOrElse(member.roles.getOrElse(Seq()))

    val isOwner = {
      if (member.isOwner.nonEmpty) member.isOwner // If the value of owner is filled, then that precedes (both in old and new format)
      else if (member.user.nonEmpty) Some(true) // Old format ==> all users become owner
      else member.isOwner // New format, take what is set
    }

    new CaseTeamMember(MemberKey(mId, member.memberType.getOrElse("user")), caseRoles = cr, isOwner = isOwner, removeRoles = member.removeRoles.getOrElse(Seq()))
  }
}
