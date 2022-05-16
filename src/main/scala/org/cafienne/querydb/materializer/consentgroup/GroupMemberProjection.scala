package org.cafienne.querydb.materializer.consentgroup

import akka.Done
import com.typesafe.scalalogging.LazyLogging
import org.cafienne.consentgroup.actorapi.event._
import org.cafienne.querydb.record.ConsentGroupMemberRecord

import scala.concurrent.Future
import scala.jdk.CollectionConverters.SetHasAsScala

class GroupMemberProjection(groupId: String, dBTransaction: ConsentGroupStorageTransaction) extends LazyLogging {
  private val rolesAdded = scala.collection.mutable.ListBuffer[ConsentGroupMemberRecord]()
  private val rolesRemoved = scala.collection.mutable.ListBuffer[ConsentGroupMemberRecord]()

  private val removedMembers = scala.collection.mutable.Set[String]()

  def handleMemberEvent(event: ConsentGroupMemberEvent): Future[Done] = {

    event match {
      case event: ConsentGroupMemberAdded =>
        // All roles plus the empty role, indicating plain group membership
        (event.member.roles ++ Set("")).foreach(role => rolesAdded += ConsentGroupMemberRecord(group = event.getActorId, userId = event.member.userId, role = role, isOwner = event.member.isOwner))
      case event: ConsentGroupMemberChanged =>
        // For changed members there is a need to add empty role as well, as it may update the consent group ownership of the user
        (event.member.roles ++ Set("")).foreach(role => rolesAdded += ConsentGroupMemberRecord(group = event.getActorId, userId = event.member.userId, role = role, isOwner = event.member.isOwner))
        event.rolesRemoved.asScala.foreach(role => rolesRemoved += ConsentGroupMemberRecord(group = event.getActorId, userId = event.member.userId, role = role, isOwner = event.member.isOwner))
      case event: ConsentGroupMemberRemoved => removedMembers.add(event.member.userId)
      case _ => // Others not known currently
    }
    Future.successful(Done)
  }

  def affectedUserIds: Set[String] = (rolesAdded.map(_.userId) ++ rolesRemoved.map(_.userId) ++ removedMembers).toSet

  def prepareCommit(): Unit = {
    rolesAdded.foreach(dBTransaction.upsert)
    rolesRemoved.foreach(dBTransaction.delete)
    removedMembers.foreach(userId => dBTransaction.deleteConsentGroupMember(groupId, userId))
  }
}
