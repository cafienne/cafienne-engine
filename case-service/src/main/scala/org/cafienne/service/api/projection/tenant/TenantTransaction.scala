package org.cafienne.service.api.projection.tenant

import akka.Done
import akka.persistence.query.Offset
import com.typesafe.scalalogging.LazyLogging
import org.cafienne.infrastructure.cqrs.OffsetRecord
import org.cafienne.service.api.projection.RecordsPersistence
import org.cafienne.service.api.tenant._
import org.cafienne.tenant.akka.event._
import org.cafienne.tenant.akka.event.platform.{PlatformEvent, TenantCreated, TenantDisabled, TenantEnabled}

import scala.collection.mutable.ListBuffer
import scala.concurrent.{ExecutionContext, Future}

class TenantTransaction(tenant: String, userQueries: UserQueries, persistence: RecordsPersistence)(implicit val executionContext: ExecutionContext) extends LazyLogging {

  val tenants = scala.collection.mutable.HashMap[String, TenantRecord]()
  val users = scala.collection.mutable.HashMap[UserRoleKey, UserRoleRecord]()

  val modifiedUsers: Iterable[String] = {
//    println("Modified: "+rolesByUserAndRoleName.keySet)
    users.keySet.map(key => key.userId).toSet
  }
  
  def handleEvent(evt: TenantEvent): Future[Done] = {
    logger.debug("Handling event of type " + evt.getClass.getSimpleName + " on tenant " + tenant)

    evt match {
      case p: PlatformEvent => handlePlatformEvent(p)
      case t: TenantUserEvent => handleUserEvent(t)
      case _ => Future.successful(Done) // Ignore other events
    }
  }

  def handlePlatformEvent(event: PlatformEvent): Future[Done] = {
    event match {
      case newTenant: TenantCreated => tenants.put(newTenant.tenantName, TenantRecord(newTenant.tenantName()))
      case disabledTenant: TenantDisabled => tenants.put(disabledTenant.tenantName(), TenantRecord(disabledTenant.tenantName(), false))
      case enabledTenant: TenantEnabled => tenants.put(enabledTenant.tenantName(), TenantRecord(enabledTenant.tenantName(), true))
    }
    Future.successful(Done)
  }

  def handleUserEvent(event: TenantUserEvent): Future[Done] = {
    val key = UserRoleKey(event)
    getUserRoleRecord(key).map(user => {
      event match {
        case t: TenantUserCreated => users.put(key, user.copy(name = t.name, email = t.email, enabled = true))
        case _: TenantUserRoleAdded => users.put(key, user.copy(enabled = true))
        case _: TenantUserRoleRemoved => users.put(key, user.copy(enabled = false))
        case _: OwnerAdded => users.put(key, user.copy(isOwner = true))
        case _: OwnerRemoved => users.put(key, user.copy(isOwner = false))
        case _: TenantUserDisabled => users.put(key, user.copy(enabled = false))
        case _: TenantUserEnabled => users.put(key, user.copy(enabled = true))
      }
      Done
    })
  }

  def commit(offsetName: String, offset: Offset): Future[Done] = {
    // Gather all records inserted/updated in this transaction, and give them for bulk update
    var records = ListBuffer.empty[AnyRef]
    this.users.values.foreach(role => records += role)
    records ++= tenants.values

    // Even if there are no new records, we will still update the offset store
    records += OffsetRecord(offsetName, offset)

    persistence.bulkUpdate(records.filter(r => r != null))
  }

  private def getUserRoleRecord(key: UserRoleKey): Future[UserRoleRecord] = {
    users.get(key) match {
      case Some(value) =>
        logger.debug(s"Retrieved user_role[$key] from current transaction cache")
        Future.successful(value)
      case None =>
        logger.debug(s"Retrieving user_role[$key] from database")
        persistence.getUserRole(key).map(result => result match {
          case Some(value) => value
          case None => UserRoleRecord(key.userId, key.tenant, key.role_name, "", "", false, true)
        })
    }
  }

}
