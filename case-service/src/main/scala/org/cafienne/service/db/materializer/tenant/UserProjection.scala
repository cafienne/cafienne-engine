package org.cafienne.service.db.materializer.tenant

import akka.Done
import com.typesafe.scalalogging.LazyLogging
import org.cafienne.identity.IdentityProvider
import org.cafienne.service.db.materializer.RecordsPersistence
import org.cafienne.service.db.record.{UserRoleKey, UserRoleRecord}
import org.cafienne.tenant.actorapi.event._

import scala.concurrent.{ExecutionContext, Future}

class UserProjection(persistence: RecordsPersistence, userCache: IdentityProvider)(implicit val executionContext: ExecutionContext) extends LazyLogging {
  private val users = scala.collection.mutable.HashMap[UserRoleKey, UserRoleRecord]()

  def handleUserEvent(event: TenantUserEvent): Future[Done] = {
    //    println("Clearing user " + event.userId +" from user cache")
    userCache.clear(event.userId)
    val key = UserRoleKey(event)
    getUserRoleRecord(key).map(user => {
      event match {
        case t: TenantUserCreated => users.put(key, user.copy(name = t.name, email = t.email, enabled = true))
        case t: TenantUserUpdated => users.put(key, user.copy(name = t.name, email = t.email, enabled = true))
        case _: TenantUserRoleAdded => users.put(key, user.copy(enabled = true))
        case _: TenantUserRoleRemoved => users.put(key, user.copy(enabled = false))
        case _: OwnerAdded => users.put(key, user.copy(isOwner = true))
        case _: OwnerRemoved => users.put(key, user.copy(isOwner = false))
        case _: TenantUserDisabled => users.put(key, user.copy(enabled = false))
        case _: TenantUserEnabled => users.put(key, user.copy(enabled = true))
        case _ => // Others not known currently
      }
      Done
    })
  }

  private def getUserRoleRecord(key: UserRoleKey): Future[UserRoleRecord] = {
    users.get(key) match {
      case Some(value) =>
        logger.debug(s"Retrieved user_role[$key] from current transaction cache")
        Future.successful(value)
      case None =>
        logger.debug(s"Retrieving user_role[$key] from database")
        persistence.getUserRole(key).map {
          case Some(value) => value
          case None => UserRoleRecord(key.userId, key.tenant, key.role_name, "", "", isOwner = false, enabled = true)
        }
    }
  }

  def prepareCommit(): Unit = {
    this.users.values.foreach(instance => persistence.upsert(instance))
  }
}
