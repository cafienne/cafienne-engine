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

package org.cafienne.timerservice.persistence.jdbc

import org.apache.pekko.Done
import org.apache.pekko.persistence.query.Offset
import org.cafienne.infrastructure.Cafienne
import org.cafienne.infrastructure.cqrs.offset.OffsetRecord
import org.cafienne.persistence.infrastructure.jdbc.CafienneJDBCConfig
import org.cafienne.persistence.infrastructure.jdbc.cqrs.JDBCOffsetStorage
import org.cafienne.timerservice.Timer
import org.cafienne.timerservice.persistence.TimerStore
import slick.basic.DatabaseConfig
import slick.jdbc.JdbcProfile

import java.time.Instant
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext, Future}

class JDBCTimerStore extends TimerStore with JDBCOffsetStorage with CafienneJDBCConfig with TimerServiceTables {
  override lazy val dbConfig: DatabaseConfig[JdbcProfile] = DatabaseConfig.forConfig(Cafienne.config.engine.timerService.store)

  import dbConfig.profile.api._

  override implicit val ec: ExecutionContext = db.ioExecutionContext

  override def getTimers(window: Instant): Future[Seq[Timer]] = {
    val query = TableQuery[TimerServiceTable].filter(_.moment <= window)
    db.run(query.distinct.result).map(records => records.map(record => Timer(record.caseInstanceId, record.timerId, record.moment, record.user)))
  }

  override def storeTimer(job: Timer, offset: Option[Offset]): Future[Done] = {
    logger.debug("Storing JDBC timer " + job.timerId + " for timestamp " + job.moment)
    val record = TimerServiceRecord(timerId = job.timerId, caseInstanceId = job.caseInstanceId, moment = job.moment, tenant = "", user = job.userId)
    commit(offset, TableQuery[TimerServiceTable].insertOrUpdate(record))
  }

  override def removeTimer(timerId: String, offset: Option[Offset]): Future[Done] = {
    logger.debug("Removing timer " + timerId)
    commit(offset, TableQuery[TimerServiceTable].filter(_.timerId === timerId).delete)
  }

  private def commit(offset: Option[Offset], action: dbConfig.profile.api.DBIO[Int]): Future[Done] = {
    val offsetUpdate = offset.map(offset => TableQuery[OffsetStoreTable].insertOrUpdate(OffsetRecord(storageName, offset)))
    val updates = offsetUpdate.fold(Seq(action))(o => Seq(action, o))
    db.run(DBIO.sequence(updates).transactionally).map(_ => Done)
  }

  override def importTimers(list: Seq[Timer]): Unit = {
    val tx = list
      .map(job => TimerServiceRecord(timerId = job.timerId, caseInstanceId = job.caseInstanceId, moment = job.moment, tenant = "", user = job.userId))
      .map(record => TableQuery[TimerServiceTable].insertOrUpdate(record))
    Await.result(db.run(DBIO.sequence(tx).transactionally), 30.seconds)
  }
}
