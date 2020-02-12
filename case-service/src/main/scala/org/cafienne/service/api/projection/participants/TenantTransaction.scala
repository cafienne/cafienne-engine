package org.cafienne.service.api.projection.participants

import akka.Done
import akka.persistence.query.Offset
import com.typesafe.scalalogging.LazyLogging
import org.cafienne.infrastructure.jdbc.OffsetStore
import org.cafienne.service.api.tenant.{Tenant, TenantOwner, UserQueries, UserRole}
import org.cafienne.service.api.projection.RecordsPersistence
import org.cafienne.tenant.akka.event.platform.{PlatformEvent, TenantCreated, TenantDisabled, TenantEnabled}
import org.cafienne.tenant.akka.event._

import scala.collection.mutable.ListBuffer
import scala.concurrent.{ExecutionContext, Future}

class TenantTransaction(tenant: String, userQueries: UserQueries, updater: RecordsPersistence)(implicit val executionContext: ExecutionContext) extends LazyLogging {

  val tenants = scala.collection.mutable.HashMap[String, Tenant]()
  val tenantOwners = scala.collection.mutable.HashMap[String, TenantOwner]()
  val rolesByUserAndRoleName = scala.collection.mutable.HashMap[(String, String), UserRole]()

  val modifiedUsers: Iterable[String] = {
//    println("Modified: "+rolesByUserAndRoleName.keySet)
    rolesByUserAndRoleName.keySet.map(key => key._1).toSet
  }

  private def storeRole(role: UserRole) = {
    val key = (role.userId, role.role_name)
//    println("Storing role "+key+": "+role)
    rolesByUserAndRoleName.put(key, role)
    Future.successful(Done)
  }

  def handleEvent(evt: TenantEvent): Future[Done] = {
    logger.debug("Handling event of type " + evt.getClass.getSimpleName + " on tenant " + tenant)

    evt match {
      case p: PlatformEvent => handlePlatformEvent(p)
      case o: OwnerAdded => addTenantOwner(o)
      case o: OwnerRemoved => removeTenantOwner(o)
      case o: TenantOwnersRequested => Future.successful(Done) // Ignore for now
      case t: TenantUserCreated => createTenantUser(t)
      case t: TenantUserDisabled => disableTenantUser(t)
      case t: TenantUserEnabled => enableTenantUser(t)
      case t: TenantUserRoleAdded => addUserRole(t)
      case t: TenantUserRoleRemoved => removeUserRole(t)
      case _ => Future.successful(Done) // Ignore other events
    }
  }

  def handlePlatformEvent(event: PlatformEvent): Future[Done] = {
    event match {
      case newTenant: TenantCreated => {
        tenants.put(newTenant.tenantName, new Tenant(newTenant.tenantName()))
        // Not much to do right here right now. We do not keep track of tenants in a database table
      }
      case disabledTenant: TenantDisabled => {
        tenants.put(disabledTenant.tenantName(), new Tenant(disabledTenant.tenantName(), false))
        // now execute a sql that disables all tenant users?
      }
      case enabledTenant: TenantEnabled => {
        tenants.put(enabledTenant.tenantName(), new Tenant(enabledTenant.tenantName(), true))
        // now execute a sql that enables all tenant users again
      }
    }
    Future.successful(Done)
  }

  def addTenantOwner(event: OwnerAdded) = {
    tenantOwners.put(event.userId, new TenantOwner(event.getActorId, event.userId))
    Future.successful(Done)
  }

  def removeTenantOwner(event: OwnerRemoved) = {
    tenantOwners.put(event.userId, new TenantOwner(event.getActorId, event.userId, false))
    Future.successful(Done)
  }

  def createTenantUser(event: TenantUserCreated): Future[Done] = {
    // Create a user with an empty role
    storeRole(UserRole(event.userId, tenant, role_name = "", name = event.name, email = event.email, enabled = true))
  }

  def disableTenantUser(event: TenantUserDisabled): Future[Done] = {
    // This should not erase the name of the user
    storeRole(UserRole(event.userId, tenant, role_name = "", name = "", email = "", enabled = false))
  }

  def enableTenantUser(event: TenantUserEnabled): Future[Done] = {
    // This should not erase the name of the user
    storeRole(UserRole(event.userId, tenant, role_name = "", name = "", email = "", enabled = true))
  }

  def addUserRole(event: TenantUserRoleAdded): Future[Done] = {
    // This should also set the name of the user? In principle not required, since user name is only taken from empty role
    storeRole(UserRole(event.userId, tenant, role_name = event.role, name = "", email = "", enabled = true))
  }

  def removeUserRole(event: TenantUserRoleRemoved): Future[Done] = {
    // Also add all roles the user has within this tenant.
    storeRole(UserRole(event.userId, tenant, role_name = event.role, name = "", email = "", enabled = false))
  }

  def commit(offsetName: String, offset: Offset): Future[Done] = {
    // Gather all records inserted/updated in this transaction, and give them for bulk update
    var records = ListBuffer.empty[AnyRef]
    this.rolesByUserAndRoleName.values.foreach(role => records += role)
    records ++= tenantOwners.values
    records ++= tenants.values

    // Even if there are no new records, we will still update the offset store
    records += OffsetStore.fromOffset(offsetName, offset)

    updater.bulkUpdate(records.filter(r => r != null))
  }
}
