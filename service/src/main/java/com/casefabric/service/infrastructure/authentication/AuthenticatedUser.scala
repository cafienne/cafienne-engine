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

package com.casefabric.service.infrastructure.authentication

import com.nimbusds.jwt.JWTClaimsSet
import com.casefabric.actormodel.identity.UserIdentity
import com.casefabric.infrastructure.serialization.Fields
import com.casefabric.json.{Value, ValueMap}

class AuthenticatedUser(override val token: String, claims: JWTClaimsSet) extends UserIdentity {
  val id: String = claims.getSubject
  UserIdentity.cacheUserToken(this.id, this.token)

  override def toValue: Value[_] = new ValueMap(Fields.userId, id)
}

object AuthenticatedUser {
  def deserialize(json: ValueMap): AuthenticatedUser = {
    // Throw an error, as probably this code is not yet in use either
    throw new NullPointerException("Deserialization of AuthenticatedUser is not yet implemented")
  }
}
