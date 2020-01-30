package org.cafienne.infrastructure.jdbc

import akka.persistence.query.Offset
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.Future

/**
  * Simple storage for event offset of a certain projection.
  */
trait OffsetStorage extends LazyLogging {
  /**
    * Gets the latest offset from the storage with the given name
    *
    * @param storageName
    * @return
    */
  def getOffset(storageName: String): Future[Offset] = ???
}

object NoOffsetStorage extends OffsetStorage {
  override def getOffset(storageName: String): Future[Offset] = Future.successful(Offset.noOffset)
}

class OffsetStorageImpl extends OffsetStorage with OffsetStoreTables with QueryDbConfig {

  import dbConfig.profile.api._

  implicit val ec = db.ioExecutionContext // TODO: Is this the best execution context to pick?
  val offsetQuery = TableQuery[OffsetStoreTable]

  /**
    * Gets the latest offset from the storage with the given name
    *
    * @param storageName
    * @return
    */
  override def getOffset(storageName: String): Future[Offset] = {
    val query = offsetQuery
      .filter(_.name === storageName)
    db.run(query.result.headOption).map(o => o match {
      case Some(value) => value.asOffset
      case None => {
        logger.debug("An offset for "+storageName+" has not been found. Starting with default 'no offset'")
        Offset.noOffset
      }
    })
  }
}

trait OffsetStoreTables extends QueryDbConfig {

  import java.sql.Timestamp

  import dbConfig.profile.api._


  final class OffsetStoreTable(tag: Tag) extends CafienneTable[OffsetStore](tag, "offset_storage") {
    def name = keyColumn[String]("name", O.PrimaryKey)

    def offsetType = column[String]("offset-type")

    def offsetValue = column[String]("offset-value")

    def timestamp = column[Timestamp]("timestamp")

    def * = (name, offsetType, offsetValue, timestamp) <> (create, OffsetStore.unapply)

    def create(t: (String, String, String, Timestamp)): OffsetStore = OffsetStore(t._1, t._2, t._3, t._4)
  }

}
