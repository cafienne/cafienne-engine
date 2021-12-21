package org.cafienne.service.db.materializer.tenant

import akka.Done
import com.typesafe.scalalogging.LazyLogging
import org.cafienne.actormodel.identity.TenantUser
import org.cafienne.service.db.materializer.RecordsPersistence
import org.cafienne.service.db.record.{UserRoleKey, UserRoleRecord}
import org.cafienne.tenant.actorapi.event.deprecated._
import org.cafienne.tenant.actorapi.event.user.{TenantMemberEvent, TenantUserAdded, TenantUserChanged, TenantUserRemoved}

import scala.concurrent.{ExecutionContext, Future}

class TenantUserProjection(persistence: RecordsPersistence)(implicit val executionContext: ExecutionContext) extends LazyLogging {
  private val deprecatedUserEventRecords = scala.collection.mutable.HashMap[UserRoleKey, UserRoleRecord]()

  private val userRolesAdded = scala.collection.mutable.ListBuffer[UserRoleRecord]()
  private val userRolesRemoved = scala.collection.mutable.ListBuffer[UserRoleRecord]()
  private val usersRemoved = scala.collection.mutable.Set[TenantUser]()

  def handleDeprecatedUserEvent(event: DeprecatedTenantUserEvent): Future[Done] = {
    //    println("Clearing user " + event.userId +" from user cache")
    val key = UserRoleKey(event)
    getUserRoleRecord(key).map(user => {
      event match {
        case t: TenantUserCreated => deprecatedUserEventRecords.put(key, user.copy(name = t.name, email = t.email, enabled = true))
        case t: TenantUserUpdated => deprecatedUserEventRecords.put(key, user.copy(name = t.name, email = t.email, enabled = true))
        case _: TenantUserRoleAdded => deprecatedUserEventRecords.put(key, user.copy(enabled = true))
        case _: TenantUserRoleRemoved => deprecatedUserEventRecords.put(key, user.copy(enabled = false))
        case _: OwnerAdded => deprecatedUserEventRecords.put(key, user.copy(isOwner = true))
        case _: OwnerRemoved => deprecatedUserEventRecords.put(key, user.copy(isOwner = false))
        case _: TenantUserDisabled => deprecatedUserEventRecords.put(key, user.copy(enabled = false))
        case _: TenantUserEnabled => deprecatedUserEventRecords.put(key, user.copy(enabled = true))
        case _ => // Others not known currently
      }
      Done
    })
  }

  private def getUserRoleRecord(key: UserRoleKey): Future[UserRoleRecord] = {
    deprecatedUserEventRecords.get(key) match {
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

  def handleUserEvent(event: TenantMemberEvent): Future[Done] = {
    val user = event.member
    event match {
      case _: TenantUserRemoved => usersRemoved += user
      case other =>
        userRolesAdded += UserRoleRecord(userId = user.id, tenant = user.tenant, role_name = "", name = user.name, email = user.email, isOwner = user.isOwner, enabled = user.enabled)
        other match {
          case _: TenantUserAdded =>
            user.roles.foreach(role => userRolesAdded += UserRoleRecord(userId = user.id, tenant = user.tenant, role_name = role, name = "", "", isOwner = false, enabled = true))
          case event: TenantUserChanged =>
            user.roles.foreach(role => userRolesAdded += UserRoleRecord(userId = user.id, tenant = user.tenant, role_name = role, name = "", "", isOwner = false, enabled = true))
            event.rolesRemoved.forEach(role => userRolesRemoved += UserRoleRecord(userId = user.id, tenant = user.tenant, role_name = role, name = "", "", isOwner = false, enabled = true))
          case _ => // Ignore others (there aren't any)
        }
    }
    Future.successful(Done)
  }

  def affectedUserIds: Set[String] = (deprecatedUserEventRecords.values ++ userRolesAdded ++ userRolesRemoved).map(_.userId).toSet ++ usersRemoved.map(_.id)

  def prepareCommit(): Unit = {
    this.deprecatedUserEventRecords.values.foreach(instance => persistence.upsert(instance))
    this.userRolesAdded.foreach(persistence.upsert)
    this.userRolesRemoved.foreach(persistence.delete)
    this.usersRemoved.foreach(persistence.deleteTenantUser)
  }
}
