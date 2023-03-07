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

package org.cafienne.infrastructure.akkahttp.authentication

import com.typesafe.scalalogging.LazyLogging
import org.cafienne.actormodel.identity.{PlatformUser, UserIdentity}
import org.cafienne.cmmn.repository.file.SimpleLRUCache
import org.cafienne.infrastructure.Cafienne
import org.cafienne.querydb.query.{TenantQueriesImpl, UserQueries}
import org.cafienne.querydb.record.TenantRecord
import org.cafienne.service.akkahttp.LastModifiedHeader

import scala.concurrent.{ExecutionContext, Future}

class IdentityCache(implicit val ec: ExecutionContext) extends IdentityProvider with LazyLogging {
  val userQueries: UserQueries = new TenantQueriesImpl

  // TODO: this should be a most recently used cache
  // TODO: check for multithreading issues now that event materializer can clear.
  private val cache = new SimpleLRUCache[String, PlatformUser](Cafienne.config.api.security.identityCacheSize)
  private val tenantCache = new SimpleLRUCache[String, TenantRecord](Cafienne.config.api.security.identityCacheSize)

  override def getPlatformUser(user: UserIdentity, tenantLastModified: LastModifiedHeader): Future[PlatformUser] = {
    tenantLastModified.available.flatMap(_ => executeUserQuery(user))
  }

  private def cacheUser(user: PlatformUser) = {
    cache.put(user.id, user)
    user
  }

  private def executeUserQuery(user: UserIdentity): Future[PlatformUser] = {
    cache.get(user.id) match {
      case user: PlatformUser => Future(user)
      case null => userQueries.getPlatformUser(user.id).map(cacheUser)
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

}
