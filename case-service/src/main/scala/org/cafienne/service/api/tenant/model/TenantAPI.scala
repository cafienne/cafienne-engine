package org.cafienne.service.api.tenant.model

import io.swagger.v3.oas.annotations.media.Schema
import org.cafienne.actormodel.identity.TenantUser
import org.cafienne.infrastructure.akka.http.EntityReader.{EntityReader, entityReader}
import org.cafienne.service.api.ApiValidator

import scala.annotation.meta.field

object TenantAPI {

  implicit val userReader: EntityReader[UserFormat] = entityReader[UserFormat]
  implicit val tenantReader: EntityReader[TenantFormat] = entityReader[TenantFormat]
  implicit val replaceTenantReader: EntityReader[ReplaceTenantFormat] = entityReader[ReplaceTenantFormat]

  case class UserFormat(
                         @(Schema @field)(implementation = classOf[String], example = "User id (matched with token when user logs on)") userId: String,
                         @(Schema @field)(description = "Option to set the list of roles the user has within the tenant") roles: Option[Set[String]],
                         @(Schema @field)(example = "Option to indicate tenant ownership changes for this user (defaults to false)", implementation = classOf[String]) isOwner: Option[Boolean],
                         @(Schema @field)(example = "Option to change the user name", implementation = classOf[String]) name: Option[String],
                         @(Schema @field)(example = "Option to change the user email", implementation = classOf[String]) email: Option[String],
                         @(Schema @field)(example = "Option to indicate whether account must be enabled/disabled (defaults to true)", implementation = classOf[String]) enabled: Option[Boolean]) {
    def asTenantUser(tenant: String): TenantUser = {
      TenantUser(id = userId, tenant = tenant, roles = roles.getOrElse(Set()), isOwner = isOwner.getOrElse(false), name = name.getOrElse(""), email = email.getOrElse(""), enabled = enabled.getOrElse(true))
    }
  }

  private def validateUserList(users: Seq[UserFormat]): Unit = {
    if (users.isEmpty) {
      throw new IllegalArgumentException("Setting tenant requires a list of users with at least one owner")
    }
    ApiValidator.runDuplicatesDetector("Tenant", "user", users.map(_.userId))
  }


  case class TenantFormat(name: String, users: Seq[UserFormat] = Seq()) {
    validateUserList(users)
    def getTenantUsers: Seq[TenantUser] = users.map(_.asTenantUser(name))
  }

  case class ReplaceTenantFormat(users: Seq[UserFormat] = Seq(), name: String = "") {
    validateUserList(users)

    def getTenantUsers(tenant: String): Seq[TenantUser] = users.map(_.asTenantUser(tenant))
  }

  case class TenantUserFormat(
    @(Schema @field)(implementation = classOf[String], example = "Same as platform user id") userId: String,
    @(Schema @field)(example = "Tenant name", implementation = classOf[String]) tenant: String,
    @(Schema @field)(description = "List of roles the user has within the tenant") roles: Set[String],
    @(Schema @field)(example = "Optional user name", implementation = classOf[String]) name: String,
    @(Schema @field)(example = "Optional user email", implementation = classOf[String]) email: String,
    @(Schema @field)(example = "Whether user is tenant owner", implementation = classOf[String]) isOwner: Boolean) {
  }

  case class PlatformUserFormat(userId: String, tenants: Seq[TenantUserFormat])
}
