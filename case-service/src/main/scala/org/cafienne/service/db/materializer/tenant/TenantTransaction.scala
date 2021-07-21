package org.cafienne.service.db.materializer.tenant

import akka.Done
import akka.persistence.query.Offset
import com.typesafe.scalalogging.LazyLogging
import org.cafienne.actormodel.event.TransactionEvent
import org.cafienne.identity.IdentityProvider
import org.cafienne.infrastructure.cqrs.OffsetRecord
import org.cafienne.service.db.materializer.RecordsPersistence
import org.cafienne.service.db.query.UserQueries
import org.cafienne.service.db.record.{TenantRecord, UserRoleKey, UserRoleRecord}
import org.cafienne.service.db.materializer.slick.SlickTransaction
import org.cafienne.tenant.actorapi.event._
import org.cafienne.tenant.actorapi.event.platform.{PlatformEvent, TenantCreated, TenantDisabled, TenantEnabled}

import scala.concurrent.{ExecutionContext, Future}

class TenantTransaction(tenant: String, userQueries: UserQueries, persistence: RecordsPersistence, userCache: IdentityProvider)(implicit val executionContext: ExecutionContext) extends SlickTransaction[TenantEvent] with LazyLogging {

  val tenants = scala.collection.mutable.HashMap[String, TenantRecord]()
  val users = scala.collection.mutable.HashMap[UserRoleKey, UserRoleRecord]()

  val modifiedUsers: Iterable[String] = {
//    println("Modified: "+rolesByUserAndRoleName.keySet)
    users.keySet.map(key => key.userId).toSet
  }

  
  def handleEvent(evt: TenantEvent, offsetName: String, offset: Offset): Future[Done] = {
    logger.debug("Handling event of type " + evt.getClass.getSimpleName + " on tenant " + tenant)

    evt match {
      case p: PlatformEvent => handlePlatformEvent(p)
      case t: TenantUserEvent => handleUserEvent(t)
      case u: TenantAppliedPlatformUpdate => updateUserIds(u, offsetName, offset)
      case _ => Future.successful(Done) // Ignore other events
    }
  }

  def handlePlatformEvent(event: PlatformEvent): Future[Done] = {
    event match {
      case newTenant: TenantCreated => tenants.put(newTenant.tenantName, TenantRecord(newTenant.tenantName()))
      case disabledTenant: TenantDisabled => tenants.put(disabledTenant.tenantName(), TenantRecord(disabledTenant.tenantName(), false))
      case enabledTenant: TenantEnabled => tenants.put(enabledTenant.tenantName(), TenantRecord(enabledTenant.tenantName(), true))
      case _ => Future.successful(Done) // Ignore other events
    }
    Future.successful(Done)
  }

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

  def updateUserIds(event: TenantAppliedPlatformUpdate, offsetName: String, offset: Offset): Future[Done] = {
    persistence.updateTenantUserInformation(event.tenant, event.newUserInformation.info, offsetName, offset)
  }

  override def commit(offsetName: String, offset: Offset, transactionEvent: TransactionEvent[_]): Future[Done] = {
    // Gather all records inserted/updated in this transaction, and give them for bulk update
    this.users.values.foreach(record => persistence.upsert(record))
    this.tenants.values.foreach(record => persistence.upsert(record))

    // Even if there are no new records, we will still update the offset store
    persistence.upsert(OffsetRecord(offsetName, offset))

    persistence.commit()
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

}
