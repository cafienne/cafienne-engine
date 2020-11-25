package org.cafienne.service.api.tenant.model

import io.swagger.v3.oas.annotations.media.Schema

import scala.annotation.meta.field

object TenantAPI {
  case class TenantFormat(name: String, users: Set[UserFormat])

  case class UpdateTenantFormat(users: Seq[UserFormat])

  case class UserFormat(
                   @(Schema @field)(implementation = classOf[String], example = "User id (matched with token when user logs on)") userId: String,
                   @(Schema @field)(description = "Option to set the list of roles the user has within the tenant") roles: Option[Seq[String]],
                   @(Schema @field)(example = "Option to indicate tenant ownership changes for this user (defaults to false)", implementation = classOf[String]) isOwner: Option[Boolean],
                   @(Schema @field)(example = "Option to change the user name", implementation = classOf[String]) name: Option[String],
                   @(Schema @field)(example = "Option to change the user email", implementation = classOf[String]) email: Option[String],
                   @(Schema @field)(example = "Option to indicate whether account must be enabled/disabled (defaults to true)", implementation = classOf[String]) enabled: Option[Boolean])

  case class TenantUserFormat(
    @(Schema @field)(implementation = classOf[String], example = "Same as platform user id") userId: String,
    @(Schema @field)(example = "Tenant name", implementation = classOf[String]) tenant: String,
    @(Schema @field)(description = "List of roles the user has within the tenant") roles: Seq[String],
    @(Schema @field)(example = "Optional user name", implementation = classOf[String]) name: String,
    @(Schema @field)(example = "Optional user email", implementation = classOf[String]) email: String,
    @(Schema @field)(example = "Whether user is tenant owner", implementation = classOf[String]) isOwner: Boolean)

  case class PlatformUserFormat(userId: String, tenants: Seq[TenantUserFormat])

  case class BackwardsCompatibleTenantFormat(name: String, owners: Option[Seq[UserFormat]] = None, users: Option[Seq[UserFormat]] = None)
}
