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

package org.cafienne.timerservice.persistence.cassandra

import com.datastax.oss.driver.api.core.`type`.DataTypes
import com.datastax.oss.driver.api.core.cql._
import com.datastax.oss.driver.api.querybuilder.QueryBuilder.literal
import com.datastax.oss.driver.api.querybuilder.{QueryBuilder, SchemaBuilder}
import org.apache.pekko.Done
import org.apache.pekko.persistence.cassandra.query.scaladsl.CassandraReadJournal
import org.apache.pekko.persistence.query.{Offset, TimeBasedUUID}
import org.apache.pekko.util.Timeout
import org.cafienne.infrastructure.cqrs.offset.OffsetRecord
import org.cafienne.system.CaseSystem
import org.cafienne.timerservice.Timer
import org.cafienne.timerservice.persistence.TimerStore

import java.time.Instant
import java.util.UUID
import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}

class CassandraTimerStore(val caseSystem: CaseSystem, readJournal: CassandraReadJournal) extends TimerStore {
  private val keyspace: String = caseSystem.config.persistence.eventDB.journal.getString("keyspace") // Use configured keyspace of journal to also store timers
  private val timerTable: String = "cafienne_timer"
  private val offsetTable: String = "cafienne_timer_offset"
  private val cassandraTimeout: Timeout = Timeout(15.seconds)
  override implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.global

  // First create the schema inside the akka keyspace.
  Await.result(readJournal.session.underlying().map(Schema.createSchema(_)), cassandraTimeout.duration)

  override def getOffset: Future[Offset] = {
    logger.debug("Reading timer offset from Cassandra")
    val offsetQuery = QueryBuilder.selectFrom(keyspace, offsetTable).all().whereColumn("name").isEqualTo(literal(storageName)).allowFiltering().build()
    readJournal.session.selectOne(offsetQuery).map(result => result.fold(Offset.noOffset)(row => TimeBasedUUID(UUID.fromString(row.getString("offset_value")))))
  }

  override def getTimers(window: Instant): Future[Seq[Timer]] = {
    logger.whenDebugEnabled(logger.debug(s"Reading existing timers from Cassandra database for window $window"))
    val select = QueryBuilder.selectFrom(keyspace, timerTable).all().whereColumn("moment").isLessThan(literal(window)).allowFiltering().build()
    readJournal.session.selectAll(select).map(rows => {
      logger.whenDebugEnabled(logger.debug("Found " + rows.length + " timers"))
      rows.map(row => {
        val timerId = row.getString("timerid")
        val caseInstanceId = row.getString("caseinstanceid")
        val tenant = row.getString("tenant")
        val userId = row.getString("user")
        val moment = row.getInstant("moment")
        if (timerId == null || caseInstanceId == null || tenant == null || userId == null || moment == null) {
          logger.error(s"Cassandra database table contains an invalid record ($timerId, $caseInstanceId, $tenant, $userId, $moment). Record will be ignored")
          null
        } else {
          Timer(caseInstanceId, timerId, moment, userId)
        }
      }).filter(_ != null) // Filter out the records that have missing column information
    })
  }

  override def storeTimer(job: Timer, offset: Option[Offset]): Future[Done] = {
    logger.whenDebugEnabled(logger.debug("Storing timer into Cassandra timer table " + job))
    val batch = BatchStatement.builder(BatchType.LOGGED).addStatement(getInsertStatement(job))
    offset.foreach(offset => batch.addStatement(getOffsetInsert(offset)))
    readJournal.session.executeWriteBatch(batch.build).map(_ => {
      logger.whenDebugEnabled(logger.debug("Stored timer " + job))
      Done
    })
  }

  private def getInsertStatement(job: Timer): SimpleStatement = {
    val insert = QueryBuilder.insertInto(keyspace, timerTable)
      .value("timerid", literal(job.timerId))
      .value("caseinstanceid", literal(job.caseInstanceId))
      .value("tenant", literal(""))
      .value("user", literal(job.userId))
      .value("moment", literal(job.moment)).build()
    insert
  }

  private def getOffsetInsert(offset: Offset): SimpleStatement = {
    val offsetRecord = OffsetRecord(storageName, offset)
    val insert = QueryBuilder.insertInto(keyspace, offsetTable)
      .value("name", literal(storageName))
      .value("offset_type", literal(offsetRecord.offsetType))
      .value("offset_value", literal(offsetRecord.offsetValue))
      .value("modified", literal(Instant.now())).build()
    insert
  }

  override def removeTimer(timerId: String, offset: Option[Offset]): Future[Done] = {
    val delete = QueryBuilder.deleteFrom(keyspace, timerTable).whereColumn("timerId").isEqualTo(literal(timerId)).build()
    logger.whenDebugEnabled(logger.debug(s"Removing timer $timerId from Cassandra database with statement $delete"))
    val batch = BatchStatement.builder(BatchType.LOGGED).addStatement(delete)
    offset.foreach(offset => batch.addStatement(getOffsetInsert(offset)))
    readJournal.session.executeWriteBatch(batch.build).map(_ => {
      logger.whenDebugEnabled(logger.debug(s"Removed timer $timerId from Cassandra database"))
      Done
    })
  }

  override def importTimers(list: Seq[Timer]): Unit = {
    val batch = BatchStatement.builder(BatchType.LOGGED)
    list.map(getInsertStatement).foreach(batch.addStatement)
    Await.result(readJournal.session.executeWriteBatch(batch.build), cassandraTimeout.duration)
  }

  object Schema {
    def createSchema(session: SyncCqlSession): Unit = {
      // Create the timer and offset table if they do not yet exist
      val timerDDL: SimpleStatement = SchemaBuilder.createTable(keyspace, timerTable).ifNotExists()
        .withPartitionKey("timerId", DataTypes.TEXT)
        .withColumn("caseInstanceId", DataTypes.TEXT)
        .withColumn("tenant", DataTypes.TEXT)
        .withColumn("user", DataTypes.TEXT)
        .withColumn("moment", DataTypes.TIMESTAMP).build()

      logger.warn(s"Creating table $timerTable: " + timerDDL)
      session.execute(timerDDL)

      val indexOnMoment: SimpleStatement = SchemaBuilder.createIndex("moment_indexed").ifNotExists().onTable(keyspace, timerTable).andColumn("moment").build()
      logger.warn(s"Adding index to column 'moment' in table $timerTable: " + indexOnMoment)
      session.execute(indexOnMoment)

      val offsetDDL: SimpleStatement = SchemaBuilder.createTable(keyspace, offsetTable).ifNotExists()
        .withPartitionKey("name", DataTypes.TEXT)
        .withColumn("offset_type", DataTypes.TEXT)
        .withColumn("offset_value", DataTypes.TEXT)
        .withColumn("modified", DataTypes.TIMESTAMP)
        .build()

      logger.warn(s"Creating table $offsetTable: " + offsetDDL)
      session.execute(offsetDDL)

      logger.info(s"Completed Cassandra table creation")
    }
  }
}
