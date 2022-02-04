package org.cafienne.infrastructure.akkahttp.route

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.{complete, onComplete}
import akka.http.scaladsl.server.Route
import org.cafienne.actormodel.identity.{Origin, PlatformUser}
import org.cafienne.cmmn.actorapi.command.team.{CaseTeam, CaseTeamGroup, CaseTeamTenantRole, CaseTeamUser}
import org.cafienne.service.db.query.exception.SearchFailure

import scala.concurrent.Future
import scala.util.{Failure, Success}

trait CaseTeamValidator extends AuthenticatedRoute with TenantValidator {
  def validateTenantAndTeam(team: CaseTeam, tenant: String, command: CaseTeam => Route): Route = {
    validateTenant(tenant, () => validateTeam(team, tenant, command))
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

  private def validateCaseTeamUsers(users: Seq[CaseTeamUser], tenant: String): Future[Seq[CaseTeamUser]] = {
    val userIds = users.map(_.userId)
    userCache.getUsers(userIds).map(platformUsers => {
      if (platformUsers.size != userIds.size) { // This will actually never happen
        val unfoundUsers = userIds.filterNot(id => platformUsers.exists(user => user.id == id))
        val msg = {
          if (unfoundUsers.size == 1) s"Cannot find a registered user '${unfoundUsers.head}'"
          else s"The users ${unfoundUsers.map(u => s"'$u'").mkString(", ")} are not registered"
        }
        throw new SearchFailure(msg)
      }
      val userInfo = platformUsers.map(u => u.id -> u).toMap[String, PlatformUser]
      users.map(caseTeamUser => caseTeamUser.copy(newOrigin = userInfo.get(caseTeamUser.userId).fold(Origin.IDP)(_.origin(tenant))))
    })
  }

  private def validateTenantRoles(tenantRoles: Seq[CaseTeamTenantRole]): Future[Seq[CaseTeamTenantRole]] = {
    // As for now no real tenant role validation, but still a hook that could be implemented to validate that
    //  there is at least one user in the tenant with the specified tenant roles
    Future.successful(tenantRoles)
  }

  private def validateConsentGroups(groups: Seq[CaseTeamGroup]): Future[Seq[CaseTeamGroup]] = {
    val groupIds = groups.map(_.groupId)
    userCache.getConsentGroups(groupIds).map(consentGroups => {
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
