package org.cafienne.service.db.materializer.cases.team

import com.typesafe.scalalogging.LazyLogging
import org.cafienne.cmmn.actorapi.command.team.{CaseTeamGroup, CaseTeamMember, CaseTeamUser, GroupRoleMapping}
import org.cafienne.cmmn.actorapi.event.team.group.{CaseTeamGroupAdded, CaseTeamGroupChanged}
import org.cafienne.cmmn.actorapi.event.team.{CaseTeamMemberEvent, CaseTeamMemberRemoved}
import org.cafienne.cmmn.instance.team.MemberType
import org.cafienne.service.db.materializer.RecordsPersistence
import org.cafienne.service.db.record.{CaseTeamGroupRecord, CaseTeamTenantRoleRecord, CaseTeamUserRecord}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext
import scala.jdk.CollectionConverters.{CollectionHasAsScala, SetHasAsScala}

class CaseTeamMemberProjection(persistence: RecordsPersistence)(implicit val executionContext: ExecutionContext) extends LazyLogging {
  private val newCaseTeamUserRoles = ListBuffer[CaseTeamUserRecord]()
  private val removedCaseTeamUserRoles = ListBuffer[CaseTeamUserRecord]()
  private val newCaseTeamTenantRoleRoles = ListBuffer[CaseTeamTenantRoleRecord]()
  private val removedCaseTeamTenantRoleRoles = ListBuffer[CaseTeamTenantRoleRecord]()
  private val newCaseTeamGroupMappings = ListBuffer[CaseTeamGroupRecord]()
  private val removedGroupMappings = ListBuffer[CaseTeamGroupRecord]()
  private val deletedMembers = mutable.Set[CaseTeamMemberKey]()

  def handleEvent(event: CaseTeamMemberEvent[_]): Unit = {
    val member: CaseTeamMember = event.member.asInstanceOf[CaseTeamMember]
    if (event.isInstanceOf[CaseTeamMemberRemoved[_]]) {
      deletedMembers += CaseTeamMemberKey(caseInstanceId = event.getActorId, memberId = member.memberId, memberType = member.memberType)
    } else if (event.member.isInstanceOf[CaseTeamGroup]) {
      handleGroupEvent(event)
    } else {
      val caseInstanceId = event.getActorId
      val memberId: String = member.memberId
      val isOwner: Boolean = member.isOwner
      val caseRoles: Set[String] = member.caseRoles ++ Set("")
      val removedRoles: Set[String] = event.getRolesRemoved.asScala.toSet

      member.memberType match {
        case MemberType.User =>
          val origin: String = member.asInstanceOf[CaseTeamUser].origin.toString
          newCaseTeamUserRoles.addAll(caseRoles.map(caseRole => CaseTeamUserRecord(caseInstanceId = caseInstanceId, tenant = event.tenant, userId = memberId, origin = origin, caseRole = caseRole, isOwner = isOwner)))
          removedCaseTeamUserRoles.addAll(removedRoles.map(caseRole => CaseTeamUserRecord(caseInstanceId = caseInstanceId, tenant = event.tenant, userId = memberId, origin = origin, caseRole = caseRole, isOwner = isOwner)))
        case MemberType.TenantRole =>
          newCaseTeamTenantRoleRoles.addAll(caseRoles.map(caseRole => CaseTeamTenantRoleRecord(caseInstanceId = caseInstanceId, tenantRole = memberId, tenant = event.tenant, caseRole = caseRole, isOwner = isOwner)))
          removedCaseTeamTenantRoleRoles.addAll(removedRoles.map(caseRole => CaseTeamTenantRoleRecord(caseInstanceId = caseInstanceId, tenantRole = memberId, tenant = event.tenant, caseRole = caseRole, isOwner = isOwner)))
        case _ => // Ignor other events
      }
    }
  }

  def handleGroupEvent(event: CaseTeamMemberEvent[_]): Unit = {
    val group = event.member.asInstanceOf[CaseTeamGroup]
    def asRecord(mapping: GroupRoleMapping): Set[CaseTeamGroupRecord] = {
      mapping.caseRoles.map(caseRole => CaseTeamGroupRecord(caseInstanceId = event.getActorId, tenant = event.tenant, groupId = group.groupId, caseRole = caseRole, groupRole = mapping.groupRole, isOwner = mapping.isOwner))
    }

    event match {
      // Add a record for each mapping
      case _: CaseTeamGroupAdded =>
        newCaseTeamGroupMappings.addAll(group.mappings.flatMap(asRecord))
      case event: CaseTeamGroupChanged =>
        newCaseTeamGroupMappings.addAll(group.mappings.flatMap(asRecord))
        removedGroupMappings.addAll(event.removedMappings.asScala.flatMap(asRecord))
      case _ => // other events are not relevant
    }
  }

  def prepareCommit(): Unit = {
    // Update case team changes
    newCaseTeamUserRoles.foreach(roleUpdate => persistence.upsert(roleUpdate))
    removedCaseTeamUserRoles.foreach(roleRemoved => persistence.delete(roleRemoved))
    newCaseTeamTenantRoleRoles.foreach(roleUpdate => persistence.upsert(roleUpdate))
    removedCaseTeamTenantRoleRoles.foreach(roleRemoval => persistence.delete(roleRemoval))
    newCaseTeamGroupMappings.foreach(groupMapping => persistence.upsert(groupMapping))
    removedGroupMappings.foreach(groupMapping => persistence.delete(groupMapping))
    deletedMembers.foreach(persistence.deleteCaseTeamMember)
  }
}

case class CaseTeamMemberKey(caseInstanceId: String, memberId: String, memberType: MemberType)