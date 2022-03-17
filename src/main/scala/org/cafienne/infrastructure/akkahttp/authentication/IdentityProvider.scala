package org.cafienne.infrastructure.akkahttp.authentication

import org.cafienne.actormodel.identity.{PlatformUser, UserIdentity}
import org.cafienne.querydb.record.TenantRecord

import scala.concurrent.Future

trait IdentityProvider {
  def getTenant(tenant: String): Future[TenantRecord] = ???

  def getPlatformUser(user: UserIdentity, tlm: Option[String]): Future[PlatformUser] = ???

  def clear(userId: String): Unit = ???
}
