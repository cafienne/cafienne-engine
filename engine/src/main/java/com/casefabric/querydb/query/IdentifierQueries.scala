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

package com.casefabric.querydb.query

import com.casefabric.actormodel.identity.UserIdentity
import com.casefabric.infrastructure.jdbc.query.{Area, Sort}
import com.casefabric.json.{CaseFabricJson, StringValue, Value}
import com.casefabric.querydb.query.filter.IdentifierFilter

import scala.concurrent.Future

trait IdentifierQueries {
  def getIdentifiers(user: UserIdentity, filter: IdentifierFilter, area: Area = Area.Default, sort: Sort = Sort.NoSort): Future[IdentifierSet] = ???

  def getIdentifierNames(user: UserIdentity, tenant: Option[String]): Future[Seq[IdentifierName]] = ???
}

class IdentifierQueriesImpl
  extends IdentifierQueries
    with BaseQueryImpl {

  import dbConfig.profile.api._

  override def getIdentifiers(user: UserIdentity, filter: IdentifierFilter, area: Area, sort: Sort): Future[IdentifierSet] = {
    val query = for {
      baseQuery <- caseIdentifiersQuery
        .filter(_.active === true)
        .filterOpt(filter.tenant)((identifier, tenant) => identifier.tenant === tenant)
        .filterOpt(filter.name)((identifier, name) => identifier.name === name)
      // Validate team membership
      _ <- membershipQuery(user, baseQuery.caseInstanceId)
    } yield baseQuery
    db.run(query.distinct.only(area).order(sort).result).map(records => {
      //      println("Found " + records.length +" matching cases on filter " + identifiers)
      IdentifierSet(records)
    })
  }

  override def getIdentifierNames(user: UserIdentity, tenant: Option[String]): Future[Seq[IdentifierName]] = {
    val query = for {
      baseQuery <- caseIdentifiersQuery
        .filter(_.active === true)
        .filterOpt(tenant)((identifier, tenant) => identifier.tenant === tenant)
      // Validate team membership
      _ <- membershipQuery(user, baseQuery.caseInstanceId)
    } yield baseQuery.name
    db.run(query.distinct.result).map(records => {
      //      println("Found " + records.length +" matching cases on filter " + identifiers)
      records.map(IdentifierName)
    })
  }
}

case class IdentifierName(name: String) extends CaseFabricJson {
  override def toValue: Value[_] = new StringValue(name)
}