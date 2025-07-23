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

import org.cafienne.actormodel.ActorType
import org.cafienne.cmmn.instance.Path
import org.cafienne.infrastructure.serialization.{Fields, JacksonSerializable}
import org.cafienne.json.{CafienneJson, Value, ValueList, ValueMap}
import org.cafienne.storage.StorageUser

case class ActorMetadata(user: StorageUser, actorType: ActorType, actorId: String, parent: ActorMetadata = null) extends JacksonSerializable with CafienneJson {
  override def toValue: Value[_] = new ValueMap(Fields.actor, toString(), Fields.path, path)

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

  private def member(memberId: String, memberType: ActorType): ActorMetadata = this.copy(actorType = memberType, actorId = memberId, parent = this)
}

object ActorMetadata {
  /**
    * Read the metadata from a "metadata": { ... } field inside the given json object,
    * and it's StorageUser from the "modelEvent": { ... } field inside the given json.
    */
  def deserializeMetadata(json: ValueMap): ActorMetadata = {
    // Read the user from the ModelEvent json
    val user = StorageUser.deserialize(json.readMap(Fields.modelEvent))
    deserializeObject(user, json.readMap(Fields.metadata))
  }

  private def deserializeObject(user: StorageUser, json: ValueMap): ActorMetadata = {
    val path = json.readString(Fields.path)

    def elementParser(element: String): ActorMetadata = parseType(element, user)

    if (path == null) {
      // Classic event
      deserialize(user, json)
    } else {
      Path.convertRawPath(path, true).map(elementParser).scan(null)((parent, next) => next.copy(parent = parent)).drop(1).reverse.head
    }
  }

  def parseType(element: String, user: StorageUser = null): ActorMetadata = {
    val openingBracket = element.indexOf("[")
    val closingBracket = element.indexOf("]")
    val actorType = ActorType.getEnum(element.substring(0, openingBracket))
    val actorId = element.substring(openingBracket + 1, closingBracket)
    ActorMetadata(user, actorType, actorId)
  }

  def deserializeChildren(metadata: ActorMetadata, jsonList: ValueList): Seq[ActorMetadata] = {
    import scala.jdk.CollectionConverters.CollectionHasAsScala
    jsonList.getValue.asScala.map(_.getValue.toString).map(s => parseType(s, metadata.user)).map(_.copy(parent = metadata)).toSeq
  }

  def deserializeChildrenStructure(metadata: ActorMetadata, jsonList: ValueList): Seq[ActorMetadata] = {
    import scala.jdk.CollectionConverters.CollectionHasAsScala
    jsonList.getValue.asScala.map(_.asMap).map(json => deserializeObject(metadata.user, json)).toSeq
  }

  /**
    * Convert a JSON object to an ActorMetadata instance
    */
  def deserialize(user: StorageUser, json: ValueMap): ActorMetadata = {
    val actorType = json.readEnum(Fields.`type`, classOf[ActorType])
    val actorId = json.readString(Fields.actorId)

    val parentActor: ActorMetadata = if (json.get(Fields.parent) != Value.NULL) {
      deserialize(user, json.readMap(Fields.parent))
    } else {
      null
    }
    ActorMetadata(user = user, actorType = actorType, actorId = actorId, parent = parentActor)
  }
}
