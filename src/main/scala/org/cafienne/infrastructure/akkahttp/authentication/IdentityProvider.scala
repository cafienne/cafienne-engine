package org.cafienne.infrastructure.akkahttp.authentication

import org.cafienne.actormodel.identity.PlatformUser
import org.cafienne.authentication.AuthenticatedUser
import org.cafienne.consentgroup.actorapi.ConsentGroup
import org.cafienne.querydb.record.TenantRecord

import scala.concurrent.Future

trait IdentityProvider {
  def getTenant(tenant: String): Future[TenantRecord] = ???

  def getPlatformUser(user: AuthenticatedUser, tlm: Option[String]): Future[PlatformUser] = ???

  def getUsers(userIds: Seq[String]): Future[Seq[PlatformUser]] = ???

  def getUserRegistration(userId: String): Future[PlatformUser] = ???

  def getConsentGroups(groupIds: Seq[String]): Future[Seq[ConsentGroup]] = ???

  def clear(userId: String): Unit = ???
}
