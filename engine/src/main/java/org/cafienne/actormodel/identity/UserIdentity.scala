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

package org.cafienne.actormodel.identity


import org.cafienne.cmmn.repository.file.SimpleLRUCache
import org.cafienne.infrastructure.Cafienne
import org.cafienne.infrastructure.serialization.{DeserializationError, Fields}
import org.cafienne.json.{CafienneJson, Value, ValueMap}

import scala.collection.immutable.HashSet

trait UserIdentity extends CafienneJson {
  val id: String

  val roles: Set[String] = new HashSet()

  override def toValue: Value[_] = {
    val json = new ValueMap(Fields.userId, id)
    if (roles.nonEmpty) {
      json.put(Fields.roles, Value.convert(roles))
    }
    json
  }

  def token: String = {
    val token = UserIdentity.tokens.get(id)
    if (token != null) {
      token
    } else {
      "" // Just return an empty string
    }
  }

  /**
    * Compatibility method for e.g. TenantUsers
    *
    * @return
    */
  def asCaseUserIdentity(): CaseUserIdentity = throw new RuntimeException(s"Cannot convert a ${this.getClass.getName} to a CaseUserIdentity, implementation is missing")
}

object UserIdentity {
  private val tokens = new SimpleLRUCache[String, String](Cafienne.config.api.security.tokenCacheSize)

  def cacheUserToken(userId: String, token: String): Unit = {
    tokens.put(userId, token)
  }

  def deserialize(json: ValueMap): UserIdentity = {
    // Note: this is a somewhat "brute-force" deserialization method, mostly required for ModelEvent, ModelResponse and ProcessCommand
    //  It is probably more clean to apply the ModelCommand.readUser also for ModelEvent, but that can be added later.
    //  Similarly, ProcessCommand then should conform to CaseUserIdentity as well.

    if (json.has(Fields.origin)) {
      // Typically from a CaseEvent
      CaseUserIdentity.deserialize(json)
    } else if (json.has(Fields.tenant)) {
      // Classic event structure
      TenantUser.deserialize(json)
    } else if (json.has(Fields.userId)) {
      // Probably DebugEvent deserialization on PlatformTenantCommand
      new UserIdentity {
        override val id: String = json.readString(Fields.userId)
      }
    } else {
      throw new DeserializationError("Cannot deserialize UserIdentity from json " + json)
    }
  }
}
