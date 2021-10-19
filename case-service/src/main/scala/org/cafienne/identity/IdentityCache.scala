package org.cafienne.identity

import com.typesafe.scalalogging.LazyLogging
import org.cafienne.actormodel.identity.{PlatformUser, TenantUser}
import org.cafienne.actormodel.response.ActorLastModified
import org.cafienne.authentication.AuthenticatedUser
import org.cafienne.cmmn.repository.file.SimpleLRUCache
import org.cafienne.infrastructure.Cafienne
import org.cafienne.service.api.tenant.TenantReader
import org.cafienne.service.db.query.UserQueries

import scala.concurrent.{ExecutionContext, Future}

trait IdentityProvider {
  def getPlatformUser(user: AuthenticatedUser, tlm: Option[String]): Future[PlatformUser] = ???

  def getUsers(userIds: Seq[String], tenant: String): Future[Seq[TenantUser]] = ???

  def clear(userId: String): Unit = ???
}

class IdentityCache(userQueries: UserQueries)(implicit val ec: ExecutionContext) extends IdentityProvider with LazyLogging {

  // TODO: this should be a most recently used cache
  // TODO: check for multithreading issues now that event materializer can clear.
  private val cache = new SimpleLRUCache[String, PlatformUser](Cafienne.config.api.security.identityCacheSize)

  override def getPlatformUser(user: AuthenticatedUser, tlm: Option[String]): Future[PlatformUser] = {
    tlm match {
      case Some(s) =>
        // Wait for the TenantReader to be informed about the tenant-last-modified timestamp
        for {
          p <- TenantReader.lastModifiedRegistration.waitFor(new ActorLastModified(s)).future
          u <- executeUserQuery(user)
        } yield (p, u)._2
      // Nothing to wait for, just continue and execute the query straight on
      case None => executeUserQuery(user)
    }
  }

  private def cacheUser(user: PlatformUser) = {
    cache.put(user.id, user)
    user
  }

  private def executeUserQuery(user: AuthenticatedUser): Future[PlatformUser] = {
    cache.get(user.userId) match {
      case user: PlatformUser => Future(user)
      case null => userQueries.getPlatformUser(user).map(cacheUser)
    }
  }

  override def clear(userId: String) = {
    // NOTE: We can also extend this to update the cache information, instead of removing keys.
    cache.remove(userId)
  }

  override def getUsers(userIds: Seq[String], tenant: String): Future[Seq[TenantUser]] = {
    userQueries.getSelectedTenantUsers(tenant, userIds)
  }
}
