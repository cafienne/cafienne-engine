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

package org.cafienne.infrastructure.http.route

import org.apache.pekko.http.scaladsl.model.StatusCodes
import org.apache.pekko.http.scaladsl.server.Route
import org.cafienne.actormodel.exception.MissingTenantException
import org.cafienne.actormodel.identity.{CaseUserIdentity, Origin, UserIdentity}
import org.cafienne.cmmn.actorapi.command.team.{CaseTeam, CaseTeamGroup, CaseTeamTenantRole, CaseTeamUser}
import org.cafienne.infrastructure.Cafienne
import org.cafienne.querydb.query.exception.SearchFailure
import org.cafienne.querydb.query.{TenantQueriesImpl, UserQueries}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

trait CaseTeamValidator extends TenantValidator {
  implicit val ec: ExecutionContext
  val userQueries: UserQueries = new TenantQueriesImpl

  def caseStarter(user: UserIdentity, optionalTenant: Option[String])(innerRoute: (CaseUserIdentity, String) => Route): Route = {
    val tenant = optionalTenant match {
      case Some(value) => if (value.isBlank) Cafienne.config.platform.defaultTenant else value
      case None => Cafienne.config.platform.defaultTenant
    }
    if (tenant.isBlank) {
      throw new MissingTenantException("Tenant field is empty or missing and a default tenant is not configured")
    }
    onComplete(getUserOrigin(user.id, tenant)) {
      case Success(user) => innerRoute(user, tenant)
      case Failure(t) => throw t
    }
  }

  def getUserOrigin(user: CaseTeamUser, tenant: String): Future[CaseTeamUser] = validateCaseTeamUsers(Seq(user), tenant).map(_.head)

  def getUserOrigin(user: String, tenant: String): Future[CaseUserIdentity] = {
    userQueries
      .determineOriginOfUsers(Seq(user), tenant)
      .map(_.headOption.fold(Origin.IDP)(_._2))
      .map(origin => CaseUserIdentity(user, origin))
  }

  def validateTenantAndTeam(team: CaseTeam, tenant: String, command: CaseTeam => Route): Route = {
    validateTenant(tenant, validateTeam(team, tenant, command))
  }

  def validateTeam(team: CaseTeam, tenant: String, command: CaseTeam => Route): Route = {
    val valid = for {
      validMembers <- validateCaseTeamUsers(team.users, tenant)
      validTenantRoles <- validateTenantRoles(team.tenantRoles)
      validGroups <- validateConsentGroups(team.groups)
    } yield (validMembers, validTenantRoles, validGroups)

    onComplete(valid) {
      case Success(usersTenantsAndGroups) => command(team.copy(users = usersTenantsAndGroups._1, tenantRoles = usersTenantsAndGroups._2, groups = usersTenantsAndGroups._3))
      case Failure(t: Throwable) => complete(StatusCodes.NotFound, t.getLocalizedMessage)
    }
  }

  def validateCaseTeamUsers(users: Seq[CaseTeamUser], tenant: String): Future[Seq[CaseTeamUser]] = {
    userQueries.determineOriginOfUsers(users.map(_.userId), tenant).map(origins => {
      val newTeam = users.map(user => {
        val origin = origins.find(_._1 == user.userId).fold(Origin.IDP)(_._2)
        user.copy(newOrigin = origin)
      })
      newTeam
    })
  }

  def validateTenantRoles(tenantRoles: Seq[CaseTeamTenantRole]): Future[Seq[CaseTeamTenantRole]] = {
    // As for now no real tenant role validation, but still a hook that could be implemented to validate that
    //  there is at least one user in the tenant with the specified tenant roles
    Future.successful(tenantRoles)
  }

  def validateConsentGroups(groups: Seq[CaseTeamGroup]): Future[Seq[CaseTeamGroup]] = {
    val groupIds = groups.map(_.groupId)
    userQueries.getConsentGroups(groupIds).map(consentGroups => {
      if (consentGroups.size != groupIds.size) {
        val unfoundGroups = groupIds.filterNot(id => consentGroups.exists(user => user.id == id))
        val msg = {
          if (unfoundGroups.size == 1) s"Cannot find a consent group with id '${unfoundGroups.head}'"
          else s"Cannot find consent groups ${unfoundGroups.map(u => s"'$u'").mkString(", ")}"
        }
        throw new SearchFailure(msg)
      }
      // Just return the source groups, no need to enrich
      groups
    })
  }
}
