/*
 * Copyright (C) 2014  Batav B.V.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.cafienne.persistence.querydb.materializer.consentgroup

import com.typesafe.scalalogging.LazyLogging
import org.cafienne.consentgroup.actorapi.event._
import org.cafienne.persistence.querydb.record.ConsentGroupMemberRecord

import scala.jdk.CollectionConverters.SetHasAsScala

class GroupMemberProjection(override val batch: ConsentGroupEventBatch) extends ConsentGroupEventMaterializer with LazyLogging {
  private val rolesAdded = scala.collection.mutable.ListBuffer[ConsentGroupMemberRecord]()
  private val rolesRemoved = scala.collection.mutable.ListBuffer[ConsentGroupMemberRecord]()

  private val removedMembers = scala.collection.mutable.Set[String]()

  def handleMemberEvent(event: ConsentGroupMemberEvent): Unit = {

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
  }

  def affectedUserIds: Set[String] = (rolesAdded.map(_.userId) ++ rolesRemoved.map(_.userId) ++ removedMembers).toSet

  def prepareCommit(): Unit = {
    rolesAdded.foreach(dBTransaction.upsert)
    rolesRemoved.foreach(dBTransaction.delete)
    removedMembers.foreach(userId => dBTransaction.deleteConsentGroupMember(groupId, userId))
  }
}
