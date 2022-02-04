package org.cafienne.infrastructure.akkahttp.authentication

import com.typesafe.scalalogging.LazyLogging
import org.cafienne.actormodel.identity.PlatformUser
import org.cafienne.actormodel.response.ActorLastModified
import org.cafienne.authentication.AuthenticatedUser
import org.cafienne.cmmn.repository.file.SimpleLRUCache
import org.cafienne.consentgroup.actorapi.ConsentGroup
import org.cafienne.infrastructure.Cafienne
import org.cafienne.querydb.materializer.tenant.TenantReader
import org.cafienne.querydb.query.UserQueries
import org.cafienne.querydb.record.TenantRecord

import scala.concurrent.{ExecutionContext, Future}

class IdentityCache(userQueries: UserQueries)(implicit val ec: ExecutionContext) extends IdentityProvider with LazyLogging {

  // TODO: this should be a most recently used cache
  // TODO: check for multithreading issues now that event materializer can clear.
  private val cache = new SimpleLRUCache[String, PlatformUser](Cafienne.config.api.security.identityCacheSize)
  private val tenantCache = new SimpleLRUCache[String, TenantRecord](Cafienne.config.api.security.identityCacheSize)

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
      case null => userQueries.getPlatformUser(user.userId).map(cacheUser)
    }
  }

  override def getTenant(tenantId: String): Future[TenantRecord] = {
    tenantCache.get(tenantId) match {
      case tenant: TenantRecord => Future(tenant)
      case null => userQueries.getTenant(tenantId).map(tenant => {
        tenantCache.put(tenantId, tenant)
        tenant
      })
    }
  }

  override def clear(userId: String): Unit = {
    // NOTE: We can also extend this to update the cache information, instead of removing keys.
    cache.remove(userId)
  }

  override def getUsers(userIds: Seq[String]): Future[Seq[PlatformUser]] = {
    userQueries.getPlatformUsers(userIds)
  }

  override def getUserRegistration(userId: String): Future[PlatformUser] = {
    userQueries.getPlatformUser(userId)
  }

  override def getConsentGroups(groupIds: Seq[String]): Future[Seq[ConsentGroup]] = {
    userQueries.getConsentGroups(groupIds)
  }
}
