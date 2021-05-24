package org.cafienne.infrastructure.jdbc

import akka.persistence.query.Offset
import org.cafienne.infrastructure.cqrs.{OffsetRecord, OffsetStorage, OffsetStorageProvider}
import org.cafienne.service.db.querydb.QueryDBSchema

import scala.concurrent.Future

class JDBCBasedOffsetStorageProvider extends OffsetStorageProvider {
  override def storage(name: String): OffsetStorage = {
    new OffsetStorageImpl(name)
  }
}

class OffsetStorageImpl(override val name: String) extends OffsetStorage with OffsetStoreTables with QueryDBSchema {

  import dbConfig.profile.api._

  implicit val ec = db.ioExecutionContext // TODO: Is this the best execution context to pick?
  val offsetQuery = TableQuery[OffsetStoreTable]

  /**
    * Gets the latest offset from the storage with the given name
    *
    * @return
    */
  override def getOffset(): Future[Offset] = {
    val query = offsetQuery
      .filter(_.name === name)
    db.run(query.result.headOption).map {
      case Some(value) => value.asOffset()
      case None => {
        logger.debug("An offset for " + name + " has not been found. Starting with default 'no offset'")
        Offset.noOffset
      }
    }
  }
}

trait OffsetStoreTables extends QueryDBSchema {

  import java.sql.Timestamp

  import dbConfig.profile.api._


  final class OffsetStoreTable(tag: Tag) extends CafienneTable[OffsetRecord](tag, "offset_storage") {
    def name = idColumn[String]("name", O.PrimaryKey)

    def offsetType = column[String]("offset-type")

    def offsetValue = column[String]("offset-value")

    def timestamp = column[Timestamp]("timestamp")

    def * = (name, offsetType, offsetValue, timestamp) <> (create, OffsetRecord.unapply)

    def create(t: (String, String, String, Timestamp)): OffsetRecord = OffsetRecord(t._1, t._2, t._3, t._4)
  }

}
