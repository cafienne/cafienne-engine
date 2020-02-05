package org.cafienne.service.api.tenant.model

import io.swagger.v3.oas.annotations.media.Schema

import scala.annotation.meta.field

object TenantAPI {
  case class Tenant(name: String, owners: Set[User])

  case class User(
                   @(Schema @field)(implementation = classOf[String], example = "User id (matched with token when user logs on)") userId: String,
                   @(Schema @field)(description = "List of roles the user has within the tenant") roles: Set[String],
                   @(Schema @field)(example = "Optional user name", implementation = classOf[String]) name: Option[String],
                   @(Schema @field)(example = "Optional user email", implementation = classOf[String]) email: Option[String])

  case class TenantUser(
    @(Schema @field)(implementation = classOf[String], example = "Same as platform user id") userId: String,
    @(Schema @field)(example = "Tenant name", implementation = classOf[String]) tenant: String,
    @(Schema @field)(description = "List of roles the user has within the tenant") roles: Seq[String],
    @(Schema @field)(example = "Optional user name", implementation = classOf[String]) name: Option[String],
    @(Schema @field)(example = "Optional user email", implementation = classOf[String]) email: Option[String])

  case class PlatformUser(userId: String, tenants: Seq[TenantUser])

}
