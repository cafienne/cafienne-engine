package org.cafienne.infrastructure.akkahttp.route

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.{complete, onComplete}
import akka.http.scaladsl.server.Route
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
      } else {
        // Now check that the case team groups have existing consent group roles (empty string is considered membership).
        val invalidGroupRoles: Seq[String] = groups.map(group => {
          val consentGroup = consentGroups.find(_.id == group.groupId).get
          val missingRoles = group.mappings.map(_.groupRole).filterNot(_.isBlank).filterNot(consentGroup.groupRoles.contains)
          if (missingRoles.isEmpty) {
            ""
          } else {
            s"Group ${group.groupId} does not have role(s) '${missingRoles.mkString("', '")}'"
          }
        }).filterNot(_.isBlank)
        if (invalidGroupRoles.nonEmpty) {
          throw new SearchFailure(invalidGroupRoles.mkString(",\n"))
        }
      }
      // If we reach this point, all consent groups have been validated, and we can return the groups.
      //  Currently no need to add extra info (an example could be the tenant to which the group belongs, or perhaps the id's of the owners)
      groups
    })
  }
}
