package org.cafienne.querydb.materializer.cases.team

import com.typesafe.scalalogging.LazyLogging
import org.cafienne.cmmn.actorapi.command.team.{CaseTeamGroup, CaseTeamMember, CaseTeamUser, GroupRoleMapping}
import org.cafienne.cmmn.actorapi.event.team.group.{CaseTeamGroupAdded, CaseTeamGroupChanged}
import org.cafienne.cmmn.actorapi.event.team.{CaseTeamMemberEvent, CaseTeamMemberRemoved}
import org.cafienne.cmmn.instance.team.MemberType
import org.cafienne.querydb.materializer.QueryDBTransaction
import org.cafienne.querydb.record.{CaseTeamGroupRecord, CaseTeamTenantRoleRecord, CaseTeamUserRecord}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext

class CaseTeamMemberProjection(persistence: QueryDBTransaction)(implicit val executionContext: ExecutionContext) extends LazyLogging {
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
      val removedRoles: Set[String] = member.rolesRemoved

      member.memberType match {
        case MemberType.User =>
          val origin: String = member.asInstanceOf[CaseTeamUser].origin.toString
          newCaseTeamUserRoles.addAll(caseRoles.map(caseRole => CaseTeamUserRecord(caseInstanceId = caseInstanceId, tenant = event.tenant, userId = memberId, origin = origin, caseRole = caseRole, isOwner = isOwner)))
          removedCaseTeamUserRoles.addAll(removedRoles.map(caseRole => CaseTeamUserRecord(caseInstanceId = caseInstanceId, tenant = event.tenant, userId = memberId, origin = origin, caseRole = caseRole, isOwner = isOwner)))
        case MemberType.TenantRole =>
          newCaseTeamTenantRoleRoles.addAll(caseRoles.map(caseRole => CaseTeamTenantRoleRecord(caseInstanceId = caseInstanceId, tenantRole = memberId, tenant = event.tenant, caseRole = caseRole, isOwner = isOwner)))
          removedCaseTeamTenantRoleRoles.addAll(removedRoles.map(caseRole => CaseTeamTenantRoleRecord(caseInstanceId = caseInstanceId, tenantRole = memberId, tenant = event.tenant, caseRole = caseRole, isOwner = isOwner)))
        case _ => // Ignore other events
      }
    }
  }

  def handleGroupEvent(event: CaseTeamMemberEvent[_]): Unit = {
    val group = event.member.asInstanceOf[CaseTeamGroup]
    def asRecords(mapping: GroupRoleMapping): Set[CaseTeamGroupRecord] = mapping.caseRoles.map(asRecord(mapping, _))
    def asRecord(mapping: GroupRoleMapping, caseRole: String): CaseTeamGroupRecord = CaseTeamGroupRecord(caseInstanceId = event.getActorId, tenant = event.tenant, groupId = group.groupId, caseRole = caseRole, groupRole = mapping.groupRole, isOwner = mapping.isOwner)

    event match {
      // Add a record for each mapping
      case _: CaseTeamGroupAdded =>
        newCaseTeamGroupMappings.addAll(group.mappings.flatMap(asRecords))
      case _: CaseTeamGroupChanged =>
        // Upsert new case roles
        newCaseTeamGroupMappings.addAll(group.mappings.flatMap(asRecords))
        // Delete removed case roles
        removedGroupMappings.addAll(group.mappings.flatMap(mapping => mapping.rolesRemoved.map(asRecord(mapping, _))))
        // Delete removed groupRoles
        removedGroupMappings.addAll(group.removedMappings.flatMap(asRecords))
      case _ => // other events are not relevant
    }
  }

  def prepareCommit(): Unit = {
    // Update case team changes. Note: order matters (a bit). So first delete, and then add new info.
    removedCaseTeamUserRoles.foreach(roleRemoved => persistence.delete(roleRemoved))
    newCaseTeamUserRoles.foreach(roleUpdate => persistence.upsert(roleUpdate))
    removedCaseTeamTenantRoleRoles.foreach(roleRemoval => persistence.delete(roleRemoval))
    newCaseTeamTenantRoleRoles.foreach(roleUpdate => persistence.upsert(roleUpdate))
    removedGroupMappings.foreach(groupMapping => persistence.delete(groupMapping))
    newCaseTeamGroupMappings.foreach(groupMapping => persistence.upsert(groupMapping))
    deletedMembers.foreach(persistence.deleteCaseTeamMember)
  }
}

case class CaseTeamMemberKey(caseInstanceId: String, memberId: String, memberType: MemberType)
