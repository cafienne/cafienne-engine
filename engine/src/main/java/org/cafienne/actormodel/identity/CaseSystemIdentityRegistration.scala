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

package org.cafienne.actormodel.identity

import com.typesafe.scalalogging.LazyLogging
import org.cafienne.persistence.infrastructure.lastmodified.LastModifiedHeader
import org.cafienne.persistence.querydb.query.tenant.TenantQueries
import org.cafienne.persistence.querydb.query.tenant.implementation.TenantQueriesImpl
import org.cafienne.persistence.querydb.record.TenantRecord
import org.cafienne.system.CaseSystem
import org.cafienne.util.SimpleLRUCache

import scala.concurrent.{ExecutionContext, Future}

class CaseSystemIdentityRegistration(caseSystem: CaseSystem)(implicit val ec: ExecutionContext) extends IdentityRegistration with LazyLogging {
  val tenantQueries: TenantQueries = new TenantQueriesImpl(caseSystem.queryDB)

  // TODO: this should be a most recently used cache
  // TODO: check for multithreading issues now that event materializer can clear.
  private val platformUserCache = new SimpleLRUCache[String, PlatformUser](caseSystem.config.api.security.identityCacheSize)
  private val tenantCache = new SimpleLRUCache[String, TenantRecord](caseSystem.config.api.security.identityCacheSize)
  private val tokens = new SimpleLRUCache[String, String](caseSystem.config.api.security.tokenCacheSize)

  override def getPlatformUser(user: UserIdentity, tenantLastModified: LastModifiedHeader): Future[PlatformUser] = {
    tenantLastModified.available.flatMap(_ => executeUserQuery(user))
  }

  override def cacheUserToken(user: UserIdentity, token: String): Unit = {
    tokens.put(user.id, token)
  }

  override def getUserToken(user: UserIdentity): String = {
    val token = tokens.get(user.id)
    if (token != null) {
      token
    } else {
      "" // Just return an empty string
    }
  }

  private def cacheUser(user: PlatformUser) = {
    platformUserCache.put(user.id, user)
    user
  }

  private def executeUserQuery(user: UserIdentity): Future[PlatformUser] = {
    platformUserCache.get(user.id) match {
      case user: PlatformUser => Future(user)
      case null => tenantQueries.getPlatformUser(user.id).map(cacheUser)
    }
  }

  override def getTenant(tenantId: String): Future[TenantRecord] = {
    tenantCache.get(tenantId) match {
      case tenant: TenantRecord => Future(tenant)
      case null => tenantQueries.getTenant(tenantId).map(tenant => {
        tenantCache.put(tenantId, tenant)
        tenant
      })
    }
  }

  override def clear(userId: String): Unit = {
    // NOTE: We can also extend this to update the cache information, instead of removing keys.
    platformUserCache.remove(userId)
  }
}
