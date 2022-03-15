package org.cafienne.infrastructure.akkahttp.authentication

import org.cafienne.actormodel.identity.{PlatformUser, UserIdentity}
import org.cafienne.consentgroup.actorapi.ConsentGroup
import org.cafienne.querydb.record.TenantRecord

import scala.concurrent.Future

trait IdentityProvider {
  def getTenant(tenant: String): Future[TenantRecord] = ???

  def getPlatformUser(user: UserIdentity, tlm: Option[String]): Future[PlatformUser] = ???

  def getUsers(userIds: Seq[String]): Future[Seq[PlatformUser]] = ???

  def getUserRegistration(userId: String): Future[PlatformUser] = ???

  def getConsentGroups(groupIds: Seq[String]): Future[Seq[ConsentGroup]] = ???

  def clear(userId: String): Unit = ???
}
