package org.cafienne.service.akkahttp.consentgroup.model

import io.swagger.v3.oas.annotations.media.Schema
import org.cafienne.consentgroup.actorapi.{ConsentGroup, ConsentGroupMember}
import org.cafienne.infrastructure.akkahttp.EntityReader.{EntityReader, entityReader}
import org.cafienne.service.akkahttp.ApiValidator
import org.cafienne.util.Guid

import scala.annotation.meta.field

object ConsentGroupAPI {

  implicit val consentGroupReader: EntityReader[ConsentGroupFormat] = entityReader[ConsentGroupFormat]
  implicit val consentGroupUserReader: EntityReader[ConsentGroupUserFormat] = entityReader[ConsentGroupUserFormat]

  case class ConsentGroupFormat(
                   @(Schema @field)(implementation = classOf[String], example = "Unique identifier of the group (optionally generated in the engine)")
                   id: Option[String],
                   members: Seq[ConsentGroupUserFormat]) {
    // Validate the list of members to not contain duplicates
    ApiValidator.runDuplicatesDetector("Consent group", "user", members.map(_.userId))

    def asGroup(tenant: String): ConsentGroup = {
      val groupId = id.fold(new Guid().toString)(id => id)
      ConsentGroup(groupId, tenant, members.map(_.asMember))
    }
  }

  case class ConsentGroupUserFormat(
                   @(Schema @field)(implementation = classOf[String], example = "User id of the consent group member")
                   userId: String,
                   @(Schema @field)(description = "Optional list of roles the user has within the consent group", example = "groupRole1, groupRole2")
                   roles: Set[String] = Set[String](),
                   @(Schema @field)(example = "Optional indicate of consent group ownership (defaults to false)", implementation = classOf[Boolean])
                   isOwner: Boolean = false) {
    def asMember: ConsentGroupMember = ConsentGroupMember(userId, roles = roles, isOwner = isOwner)
  }
}
