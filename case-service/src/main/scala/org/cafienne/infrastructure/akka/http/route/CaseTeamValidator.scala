package org.cafienne.infrastructure.akka.http.route

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.{complete, onComplete}
import akka.http.scaladsl.server.Route
import org.cafienne.actormodel.identity.{Origin, PlatformUser}
import org.cafienne.cmmn.actorapi.command.team.{CaseTeam, CaseTeamTenantRole, CaseTeamUser}
import org.cafienne.identity.IdentityProvider
import org.cafienne.service.db.query.exception.SearchFailure

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

trait CaseTeamValidator extends AuthenticatedRoute {
  implicit val userCache: IdentityProvider
  implicit val ec: ExecutionContext

  def validateTeam(team: CaseTeam, tenant: String, command: CaseTeam => Route): Route = {
    val valid = for {
      validMembers <- validateCaseTeamUsers(team.users, tenant)
      validTenantRoles <- validateTenantRoles(team.tenantRoles)
    } yield (validMembers, validTenantRoles)

    onComplete(valid) {
      case Success(usersTenantsAndGroups) => command(team.copy(users = usersTenantsAndGroups._1, tenantRoles = usersTenantsAndGroups._2))
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
}
