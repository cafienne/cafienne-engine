package org.cafienne.identity

import com.typesafe.scalalogging.LazyLogging
import org.cafienne.akka.actor.identity.PlatformUser
import org.cafienne.service.api.participants.UserQueries

import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}

trait IdentityProvider {
  def getUser(userId: String): Future[PlatformUser]
  def clear(users: Iterable[String]): Unit
}

class IdentityCache(userQueries: UserQueries)(implicit val ec: ExecutionContext) extends IdentityProvider with LazyLogging {

  // TODO: this should be a most recently used cache
  // TODO: check for multithreading issues now that event materializer can clear.
  private val cache = new mutable.HashMap[String, PlatformUser]

  def getUser(userId: String): Future[PlatformUser] = {
    if (true == true) {
      // TODO: FOR NOW CACHE IS DISABLED, since TenantProjection does not properly clear the cache.
      return executeUserQuery(userId)
    }

//    System.err.println("Fetching user " + userId + ", cache contents: " + cache)
    cache.get(userId) match {
      case Some(user) => {
//        System.out.println("\n\nFound cache hit for " + userId + "\n")
        Future {
          user
        }
      }
      case None => executeUserQuery(userId)
    }
  }

  private def executeUserQuery(userId: String): Future[PlatformUser] = {
    userQueries.getPlatformUser(userId).map(u => {
      if (u.users.isEmpty && !u.isPlatformOwner) {
        logger.info("User " + userId + " has a valid token, but is not registered in the case system")
        throw new SecurityException("User " + userId + " is not registered in the case system")
      }
      cache.put(userId, u)
      u
    })
  }

  def clear(users: Iterable[String]): Unit = {
    // NOTE: We can also extend this to update the cache information, instead of removing keys.
    users.foreach(userId => cache.remove(userId))
  }
}
