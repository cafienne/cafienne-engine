package org.cafienne.identity

import com.typesafe.scalalogging.LazyLogging
import org.cafienne.akka.actor.command.response.ActorLastModified
import org.cafienne.akka.actor.identity.{PlatformUser, TenantUser}
import org.cafienne.service.api.projection.query.UserQueries
import org.cafienne.service.api.tenant.TenantReader

import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}

trait IdentityProvider {
  def getUser(userId: String, tlm: Option[String]): Future[PlatformUser]
  def getUsers(userIds: Seq[String], tenant: String): Future[Seq[TenantUser]] = ???
  def clear(users: Iterable[String]): Unit
}

class IdentityCache(userQueries: UserQueries)(implicit val ec: ExecutionContext) extends IdentityProvider with LazyLogging {

  // TODO: this should be a most recently used cache
  // TODO: check for multithreading issues now that event materializer can clear.
  private val cache = new mutable.HashMap[String, PlatformUser]

  def getUser(userId: String, tlm: Option[String]): Future[PlatformUser] = {
    if (true == true) {
      // TODO: FOR NOW CACHE IS DISABLED, since TenantProjection does not properly clear the cache.
      tlm match {
        case Some(s) => {
            // Now go to the writer and ask it to wait for the clm for this case instance id...
            val user = for {
              p <- TenantReader.lastModifiedRegistration.waitFor(new ActorLastModified(s)).future
              u <- executeUserQuery(userId).flatMap(u => Future(u)) // Not really sure why flatmap is required. Becuase the executeUserQuery returns a Future, i guess
            } yield u

          return user
          }

        case None => // Nothing to do, just continue
          executeUserQuery(userId)
      }
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

  override def getUsers(userIds: Seq[String], tenant: String): Future[Seq[TenantUser]] = {
    userQueries.getSelectedTenantUsers(tenant, userIds)
  }
}
