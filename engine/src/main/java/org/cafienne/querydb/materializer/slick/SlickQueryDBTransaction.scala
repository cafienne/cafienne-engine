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

package org.cafienne.querydb.materializer.slick

import org.cafienne.cmmn.actorapi.command.platform.NewUserInformation
import org.cafienne.infrastructure.cqrs.offset.OffsetRecord
import org.cafienne.infrastructure.jdbc.cqrs.OffsetStoreTables
import org.cafienne.querydb.materializer.QueryDBTransaction
import org.cafienne.querydb.schema.QueryDBSchema
import org.cafienne.querydb.schema.table.{CaseTables, ConsentGroupTables, TaskTables, TenantTables}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.concurrent.{Await, ExecutionContext}

class SlickQueryDBTransaction
  extends QueryDBTransaction
    with QueryDBSchema
    with CaseTables
    with TaskTables
    with TenantTables
    with ConsentGroupTables
    with OffsetStoreTables {

  import dbConfig.profile.api._

  implicit val ec: ExecutionContext = db.ioExecutionContext // TODO: Is this the best execution context to pick?

  private val dbStatements: mutable.ListBuffer[DBIO[_]] = ListBuffer[DBIO[_]]()

  def addStatement(action: dbConfig.profile.api.DBIO[_]): Unit = dbStatements += action

  private val DB_TIMEOUT: FiniteDuration = 21.seconds

  def runSync[R](action: DBIOAction[R, NoStream, Nothing]): R = Await.result(db.run(action), DB_TIMEOUT)

  override def upsert(record: OffsetRecord): Unit = addStatement(TableQuery[OffsetStoreTable].insertOrUpdate(record))

  def commit(): Unit = {
    val transaction = dbStatements.toSeq
    // Clear statement buffer (the "transaction")
    dbStatements.clear()
    // Run the actions
    runSync(DBIO.sequence(transaction).transactionally)
  }

  def convertUserUpdate(info: Seq[NewUserInformation]): Set[(String, Set[String])] = {
    val newUserIds: Set[String] = info.map(_.newUserId).toSet
    newUserIds.map(newUserId => (newUserId, info.filter(_.newUserId == newUserId).map(_.existingUserId).toSet))
  }

  //  var nr = 0L
  def addOffsetRecord(offset: OffsetRecord): Seq[DBIO[_]] = {
    //    println(s"$nr: Updating $offsetName to $offset")
    //    nr += 1
    Seq(TableQuery[OffsetStoreTable].insertOrUpdate(offset))
  }
}
