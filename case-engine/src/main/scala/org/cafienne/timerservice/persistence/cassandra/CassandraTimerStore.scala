package org.cafienne.timerservice.persistence.cassandra

import akka.Done
import akka.persistence.cassandra.query.scaladsl.CassandraReadJournal
import akka.persistence.query.{Offset, TimeBasedUUID}
import akka.util.Timeout
import com.datastax.driver.core.querybuilder.{Insert, QueryBuilder}
import com.datastax.driver.core.schemabuilder.SchemaBuilder
import com.datastax.driver.core.{BatchStatement, DataType}
import org.cafienne.actormodel.identity.TenantUser
import org.cafienne.infrastructure.cqrs.OffsetRecord
import org.cafienne.timerservice.Timer
import org.cafienne.timerservice.persistence.TimerStore

import java.util.UUID
import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}

class CassandraTimerStore(readJournal: CassandraReadJournal) extends TimerStore {
  val keyspace: String = "akka" // For now store timers inside the akka keyspace, not too sure if that is handy
  val timerTable = "cafienne_timer"
  val offsetTable = "cafienne_timer_offset"
  val cassandraTimeout = Timeout(15.seconds)
  override implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.global

  Await.result({
    // Register codec for Instant <-> timestamp
    readJournal.session.underlying().map(s => s.getCluster.getConfiguration.getCodecRegistry.register(com.datastax.driver.extras.codecs.jdk8.InstantCodec.instance))

    // Create the timer and offst table if they do not yet exist
    val timerDDL = SchemaBuilder.createTable(keyspace, timerTable).ifNotExists()
      .addPartitionKey("timerId", DataType.text())
      .addColumn("caseInstanceId", DataType.text())
      .addColumn("tenant", DataType.text())
      .addColumn("user", DataType.text())
      .addColumn("moment", DataType.timestamp())
    val offsetDDL = SchemaBuilder.createTable(keyspace, offsetTable).ifNotExists()
      .addPartitionKey("name", DataType.text())
      .addColumn("offset_type", DataType.text())
      .addColumn("offset_value", DataType.text())
      .addColumn("modified", DataType.timestamp())

    logger.info(s"Creating table $timerTable: " + timerDDL)
    readJournal.session.executeDDL(timerDDL.toString)
    logger.info(s"Creating table $offsetTable: " + offsetDDL)
    readJournal.session.executeDDL(offsetDDL.toString)

  }, cassandraTimeout.duration)
  logger.info(s"Completed Cassandra table creation")

  override def getOffset(): Future[Offset] = {
    logger.debug("Reading timer offset from Cassandra")
    readJournal.session.selectOne(s"SELECT offset_value FROM $keyspace.$offsetTable where name = ?", storageName).map(result => result.fold(Offset.noOffset)(row => {
      val offsetValue = row.getString("offset_value")
      TimeBasedUUID(UUID.fromString(offsetValue))
    }))
  }

  override def getTimers(): Future[Seq[Timer]] = {
    logger.whenDebugEnabled(logger.debug("Reading existing timers from Cassandra database"))
    val select = QueryBuilder.select().from(keyspace, timerTable)
    readJournal.session.selectAll(select).map(rows => {
      logger.whenDebugEnabled(logger.debug("Found " + rows.length + " timers"))
      rows.map(row => {
        val timerId = row.getString("timerid")
        val caseInstanceId = row.getString("caseinstanceid")
        val tenant = row.getString("tenant")
        val userId = row.getString("user")
        val moment = row.getTimestamp("moment")
        if (timerId == null || caseInstanceId == null || tenant == null || userId == null || moment == null) {
          logger.error(s"Cassandra database table contains an invalid record ($timerId, $caseInstanceId, $tenant, $userId, $moment). Record will be ignored")
          null
        } else {
          Timer(caseInstanceId, timerId, moment.toInstant, new TenantUser(userId, Seq(), tenant, false, ""))
        }
      }).filter(timer => timer != null) // Filter out the records that have missing column information
    })
  }

  override def storeTimer(job: Timer, offset: Option[Offset]): Future[Done] = {
    logger.whenDebugEnabled(logger.debug("Storing timer into Cassandra timer table " + job))
    val batch = new BatchStatement()
    batch.add(getInsertStatement(job))
    addOffsetStatement(batch, offset)
    readJournal.session.executeWriteBatch(batch).map(_ => {
      logger.whenDebugEnabled(logger.debug("Stored timer " + job))
      Done
    })
  }

  private def getInsertStatement(job: Timer): Insert = {
    QueryBuilder.insertInto(keyspace, "cafienne_timer")
      .value("timerid", job.timerId)
      .value("caseinstanceid", job.caseInstanceId)
      .value("tenant", job.user.tenant)
      .value("user", job.user.id)
      .value("moment", job.moment)
  }

  private def addOffsetStatement(batch: BatchStatement, optionalOffset: Option[Offset]): Unit = {
    optionalOffset.map(offset => {
      val offsetRecord = OffsetRecord(storageName, offset)
      val record = QueryBuilder.insertInto(keyspace, "cafienne_timer_offset")
        .value("name", storageName)
        .value("offset_type", offsetRecord.offsetType)
        .value("offset_value", offsetRecord.offsetValue)
        .value("modified", offsetRecord.timestamp)
      batch.add(record)
    })
  }

  override def removeTimer(timerId: String, offset: Option[Offset]): Future[Done] = {
    val delete = QueryBuilder.delete().all().from(keyspace, timerTable).where(QueryBuilder.eq("timerId", timerId))
    logger.whenDebugEnabled(logger.debug(s"Removing timer $timerId from Cassandra database with statement $delete"))
    val batch = new BatchStatement()
    batch.add(delete)
    addOffsetStatement(batch, offset)
    readJournal.session.executeWriteBatch(batch).map(_ => {
      logger.whenDebugEnabled(logger.debug(s"Removed timer $timerId from Cassandra database"))
      Done
    })
  }

  override def importTimers(list: Seq[Timer]): Unit = {
    val batch = new BatchStatement()
    list.map(getInsertStatement).foreach(statement => batch.add(statement))
    Await.result(readJournal.session.executeWriteBatch(batch), cassandraTimeout.duration)
  }
}
