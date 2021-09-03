package org.cafienne.service.db.materializer.tenant

import akka.Done
import akka.persistence.query.Offset
import com.typesafe.scalalogging.LazyLogging
import org.cafienne.actormodel.event.CommitEvent
import org.cafienne.identity.IdentityProvider
import org.cafienne.infrastructure.cqrs.{ModelEventEnvelope, OffsetStorage}
import org.cafienne.service.api.tenant.TenantReader
import org.cafienne.service.db.materializer.RecordsPersistence
import org.cafienne.service.db.materializer.slick.SlickTransaction
import org.cafienne.service.db.record.{TenantRecord, UserRoleKey, UserRoleRecord}
import org.cafienne.tenant.actorapi.event._
import org.cafienne.tenant.actorapi.event.platform.{PlatformEvent, TenantCreated, TenantDisabled, TenantEnabled}

import scala.concurrent.{ExecutionContext, Future}

class TenantTransaction(tenant: String, persistence: RecordsPersistence, userCache: IdentityProvider, offsetStorage: OffsetStorage)
                       (implicit val executionContext: ExecutionContext) extends SlickTransaction with LazyLogging {

  val tenants = scala.collection.mutable.HashMap[String, TenantRecord]()
  val users = scala.collection.mutable.HashMap[UserRoleKey, UserRoleRecord]()

  val modifiedUsers: Iterable[String] = {
//    println("Modified: "+rolesByUserAndRoleName.keySet)
    users.keySet.map(key => key.userId).toSet
  }

  override def handleEvent(envelope: ModelEventEnvelope): Future[Done] = {
    logger.debug("Handling event of type " + envelope.event.getClass.getSimpleName + " on tenant " + tenant)

    envelope.event match {
      case p: PlatformEvent => handlePlatformEvent(p)
      case t: TenantUserEvent => handleUserEvent(t)
      case _ => Future.successful(Done) // Ignore other events
    }
  }

  def handlePlatformEvent(event: PlatformEvent): Future[Done] = {
    event match {
      case newTenant: TenantCreated => tenants.put(newTenant.tenantName, TenantRecord(newTenant.tenantName()))
      case disabledTenant: TenantDisabled => tenants.put(disabledTenant.tenantName(), TenantRecord(disabledTenant.tenantName(), enabled = false))
      case enabledTenant: TenantEnabled => tenants.put(enabledTenant.tenantName(), TenantRecord(enabledTenant.tenantName(), enabled = true))
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

  override def commit(envelope: ModelEventEnvelope, transactionEvent: CommitEvent): Future[Done] = {
    transactionEvent match {
      case event: TenantModified => commitTenantRecords(envelope, event)
      case event: TenantAppliedPlatformUpdate => updateUserIds(event, envelope.offset)
      case _ =>
        logger.warn(s"TenantTransaction unexpectedly receives a commit event of type ${transactionEvent.getClass.getName}. This event is ignored.")
        Future.successful(Done)
    }
  }

  private def commitTenantRecords(envelope: ModelEventEnvelope, tenantModified: TenantModified): Future[Done] = {
    // Gather all records inserted/updated in this transaction, and give them for bulk update
    this.users.values.foreach(record => persistence.upsert(record))
    this.tenants.values.foreach(record => persistence.upsert(record))
    // Even if there are no new records, we will still update the offset store
    persistence.upsert(offsetStorage.createOffsetRecord(envelope.offset))
    persistence.commit().andThen(_ => TenantReader.lastModifiedRegistration.handle(tenantModified))
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

  private def updateUserIds(event: TenantAppliedPlatformUpdate, offset: Offset): Future[Done] = {
    persistence.updateTenantUserInformation(event.tenant, event.newUserInformation.info, offsetStorage.createOffsetRecord(offset))
  }

}
