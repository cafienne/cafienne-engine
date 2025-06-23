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

package org.cafienne.persistence.querydb.query.cmmn

import org.cafienne.actormodel.identity.UserIdentity
import org.cafienne.persistence.infrastructure.jdbc.query.{Area, Sort}
import org.cafienne.persistence.querydb.query.cmmn.filter.CaseFilter
import org.cafienne.persistence.querydb.record._

import scala.concurrent.Future

trait CaseListQueries {

  //  def getCasesStats(user: UserIdentity, tenant: Option[String], from: Int, numOfResults: Int, caseName: Option[String], status: Option[String]): Future[Seq[CaseList]] = ??? // GetCaseList

  def getCases(user: UserIdentity, filter: CaseFilter, area: Area = Area.Default, sort: Sort = Sort.NoSort): Future[Seq[CaseRecord]]
}
