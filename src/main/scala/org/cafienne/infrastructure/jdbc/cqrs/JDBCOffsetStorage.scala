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

package org.cafienne.infrastructure.jdbc.cqrs

import akka.persistence.query.Offset
import org.cafienne.infrastructure.cqrs.offset.{OffsetRecord, OffsetStorage}
import org.cafienne.infrastructure.jdbc.CafienneJDBCConfig

import scala.concurrent.{ExecutionContext, Future}

trait JDBCOffsetStorage extends OffsetStorage with OffsetStoreTables with CafienneJDBCConfig {

  val storageName: String

  import dbConfig.profile.api._

  implicit val ec: ExecutionContext = db.ioExecutionContext

  val offsetQuery = TableQuery[OffsetStoreTable]

  /**
    * Gets the latest offset from the storage with the given name
    *
    * @return
    */
  override def getOffset: Future[Offset] = {
    val query = offsetQuery.filter(_.name === storageName)
    db.run(query.result.headOption).map {
      case Some(value) => value.asOffset()
      case None =>
        logger.debug("An offset for " + storageName + " has not been found. Starting with default 'no offset'")
        Offset.noOffset
    }
  }
}

trait OffsetStoreTables extends CafienneJDBCConfig {
  import dbConfig.profile.api._

  import java.sql.Timestamp

  final class OffsetStoreTable(tag: Tag) extends CafienneTable[OffsetRecord](tag, "offset_storage") {
    def name = idColumn[String]("name", O.PrimaryKey)

    def offsetType = column[String]("offset-type")

    def offsetValue = column[String]("offset-value")

    def timestamp = column[Timestamp]("timestamp")

    def * = (name, offsetType, offsetValue, timestamp).<>(create, OffsetRecord.unapply)

    def create(t: (String, String, String, Timestamp)): OffsetRecord = OffsetRecord(t._1, t._2, t._3, t._4)
  }
}