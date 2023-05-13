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

package org.cafienne.storage.actormodel

import org.cafienne.cmmn.instance.Case
import org.cafienne.consentgroup.ConsentGroupActor
import org.cafienne.infrastructure.serialization.{Fields, JacksonSerializable}
import org.cafienne.json.{CafienneJson, Value, ValueMap}
import org.cafienne.processtask.instance.ProcessTaskActor
import org.cafienne.storage.StorageUser
import org.cafienne.tenant.TenantActor

case class ActorMetadata(user: StorageUser, actorType: String, tenant: String, actorId: String, parent: ActorMetadata = null) extends JacksonSerializable with CafienneJson {
  override def toValue: Value[_] = new ValueMap(Fields.user, user, Fields.`type`, actorType, Fields.actorId, actorId, Fields.tenant, tenant, Fields.parent, parent)

  def path: String = {
    if (hasParent) {
      s"${parent.path}/$actorType[$actorId]"
    } else {
      s"$actorType[$actorId]"
    }
  }

  val hasParent: Boolean = parent != null

  val isRoot: Boolean = !hasParent

  override def toString: String = s"$actorType[$actorId]"

  def processMember(processId: String): ActorMetadata = member(processId, ActorType.Process)

  def caseMember(caseId: String): ActorMetadata = member(caseId, ActorType.Case)

  def groupMember(groupId: String): ActorMetadata = member(groupId, ActorType.Group)


  private def member(memberId: String, memberType: String): ActorMetadata =
    this.copy(actorType = memberType, actorId = memberId, parent = this)
}

object ActorMetadata {
  /**
   * Read the metadata from a "metadata": { ... } field inside the given json object
   */
  def deserializeMetadata(json: ValueMap): ActorMetadata = {
    deserialize(json.readMap(Fields.metadata))
  }

  /**
   * Convert a JSON object to an ActorMetadata instance
   */
  def deserialize(json: ValueMap): ActorMetadata = {
    val user = StorageUser.deserialize(json.readMap(Fields.user))
    val actorType = json.readString(Fields.`type`)
    val actorId = json.readString(Fields.actorId)
    val tenant = json.readString(Fields.tenant)
    val parentActor: ActorMetadata = if (json.get(Fields.parent) != Value.NULL) {
      deserialize(json.readMap(Fields.parent))
    } else {
      null
    }
    ActorMetadata(user = user, actorType = actorType, actorId = actorId, tenant = tenant, parent = parentActor)
  }
}

object ActorType {
  val Case: String = classOf[Case].getSimpleName
  val Process: String = classOf[ProcessTaskActor].getSimpleName
  val Group: String = classOf[ConsentGroupActor].getSimpleName
  val Tenant: String = classOf[TenantActor].getSimpleName
}
