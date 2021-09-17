package org.cafienne.service.db.materializer.cases.team

import akka.Done
import com.typesafe.scalalogging.LazyLogging
import org.cafienne.cmmn.actorapi.event.team._
import org.cafienne.cmmn.actorapi.event.team.deprecated.{DeprecatedCaseTeamEvent, TeamMemberAdded}
import org.cafienne.cmmn.actorapi.event.team.member.{CaseOwnerAdded, CaseOwnerRemoved, CaseTeamMemberEvent, TeamRoleCleared, TeamRoleFilled}
import org.cafienne.service.db.materializer.RecordsPersistence
import org.cafienne.service.db.record.CaseTeamMemberRecord

import scala.concurrent.{ExecutionContext, Future}

class CaseTeamProjection(persistence: RecordsPersistence)(implicit val executionContext: ExecutionContext) extends LazyLogging {
  private val caseInstanceTeamMembers = scala.collection.mutable.HashMap[(String, String, Boolean, String), CaseTeamMemberRecord]()

  def handleCaseTeamEvent(event: CaseTeamEvent): Future[Done] = {
    // We handle 2 types of event: either the old ones (which carried all info in one shot) or the new ones, which are more particular
    event match {
      case event: DeprecatedCaseTeamEvent => {
        // Deprecated case team events have all member roles in them; these members are always of type user; all those users become owner and active;
        import scala.jdk.CollectionConverters._
        // We need to add the empty role (if not yet there),
        //  in order to have member table also populated when a member has no roles but still is part of the team
        val roles = event.getRoles().asScala ++ Seq("")
        // Now determine whether the user (and it's roles) become active (and then also owner) or de-activated
        val enabled = if (event.isInstanceOf[TeamMemberAdded]) true else false // Both for ownership and active
        // For reach role add a record.
        roles.map(role => {
          val key = (event.getActorId, event.getUserId, true, role)
          val record = CaseTeamMemberRecord(event.getActorId, tenant = event.tenant, memberId = event.getUserId, caseRole = role, isTenantUser = true, isOwner = enabled, active = enabled)
          caseInstanceTeamMembers.put(key, record)
        })
      }
      // New type of event:
      case event: CaseTeamMemberEvent => {
        val key = (event.getActorId, event.memberId, event.isTenantUser, event.roleName)
        // Make sure to update any existing versions of the record (especially if first a user is added and at the same time becomes owner this is necessary)
        //  We have seen situation with SQL Server where the order of the update actually did not make a user owner
        val member = caseInstanceTeamMembers.getOrElseUpdate(key, CaseTeamMemberRecord(event.getActorId, tenant = event.tenant, caseRole = event.roleName, isTenantUser = event.isTenantUser, memberId = event.memberId, isOwner = false, active = true))
        event match {
          case _: TeamRoleFilled => caseInstanceTeamMembers.put(key, member.copy(active = true))
          case _: TeamRoleCleared => caseInstanceTeamMembers.put(key, member.copy(active = false))
          case _: CaseOwnerAdded => caseInstanceTeamMembers.put(key, member.copy(isOwner = true))
          case _: CaseOwnerRemoved => caseInstanceTeamMembers.put(key, member.copy(isOwner = false))
          case _ => // Ignore other events
        }
      }
      case _ => // Ignore other events
    }
    Future.successful(Done)
  }

  def prepareCommit(): Unit = {
    // Update case team changes
    this.caseInstanceTeamMembers.values.foreach(item => persistence.upsert(item))
  }
}
