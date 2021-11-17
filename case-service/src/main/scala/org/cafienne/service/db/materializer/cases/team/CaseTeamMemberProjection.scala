package org.cafienne.service.db.materializer.cases.team

import com.typesafe.scalalogging.LazyLogging
import org.cafienne.cmmn.actorapi.command.team.{CaseTeamMember, CaseTeamUser}
import org.cafienne.cmmn.actorapi.event.team.{CaseTeamMemberEvent, CaseTeamMemberRemoved}
import org.cafienne.cmmn.instance.team.MemberType
import org.cafienne.service.db.materializer.RecordsPersistence
import org.cafienne.service.db.record.{CaseTeamTenantRoleRecord, CaseTeamUserRecord}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext
import scala.jdk.CollectionConverters.SetHasAsScala

class CaseTeamMemberProjection(persistence: RecordsPersistence)(implicit val executionContext: ExecutionContext) extends LazyLogging {
  private val newCaseTeamUserRoles = ListBuffer[CaseTeamUserRecord]()
  private val removedCaseTeamUserRoles = ListBuffer[CaseTeamUserRecord]()
  private val newCaseTeamTenantRoleRoles = ListBuffer[CaseTeamTenantRoleRecord]()
  private val removedCaseTeamTenantRoleRoles = ListBuffer[CaseTeamTenantRoleRecord]()
  private val deletedMembers = mutable.Set[CaseTeamMemberKey]()

  def handleEvent(event: CaseTeamMemberEvent[_]): Unit = {
    val member: CaseTeamMember = event.member.asInstanceOf[CaseTeamMember]
    if (event.isInstanceOf[CaseTeamMemberRemoved[_]]) {
      deletedMembers += CaseTeamMemberKey(caseInstanceId = event.getActorId, memberId = member.memberId, memberType = member.memberType)
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
        case _ => // Ignore other events
      }
    }
  }

  def prepareCommit(): Unit = {
    // Update case team changes
    newCaseTeamUserRoles.foreach(roleUpdate => persistence.upsert(roleUpdate))
    removedCaseTeamUserRoles.foreach(roleRemoved => persistence.delete(roleRemoved))
    newCaseTeamTenantRoleRoles.foreach(roleUpdate => persistence.upsert(roleUpdate))
    removedCaseTeamTenantRoleRoles.foreach(roleRemoval => persistence.delete(roleRemoval))
    deletedMembers.foreach(persistence.deleteCaseTeamMember)
  }
}

case class CaseTeamMemberKey(caseInstanceId: String, memberId: String, memberType: MemberType)