package org.cafienne.service.api.tenant.model

import io.swagger.v3.oas.annotations.media.Schema

import scala.annotation.meta.field

object TenantAPI {
  case class TenantFormat(name: String, users: Set[UserFormat])

  case class UserFormat(
                   @(Schema @field)(implementation = classOf[String], example = "User id (matched with token when user logs on)") userId: String,
                   @(Schema @field)(description = "List of roles the user has within the tenant") roles: Seq[String],
                   @(Schema @field)(example = "Whether user is tenant owner (defaults to false, but there must be at least one user in the list  with true)", implementation = classOf[String]) isOwner: Option[Boolean],
                   @(Schema @field)(example = "Optional user name", implementation = classOf[String]) name: Option[String],
                   @(Schema @field)(example = "Optional user email", implementation = classOf[String]) email: Option[String])

  case class TenantUserFormat(
    @(Schema @field)(implementation = classOf[String], example = "Same as platform user id") userId: String,
    @(Schema @field)(example = "Tenant name", implementation = classOf[String]) tenant: String,
    @(Schema @field)(description = "List of roles the user has within the tenant") roles: Seq[String],
    @(Schema @field)(example = "Optional user name", implementation = classOf[String]) name: Option[String],
    @(Schema @field)(example = "Optional user email", implementation = classOf[String]) email: Option[String])

  case class PlatformUserFormat(userId: String, tenants: Seq[TenantUserFormat])

  case class BackwardsCompatibleTenantFormat(name: String, owners: Option[Seq[UserFormat]] = None, users: Option[Seq[UserFormat]] = None)
}
