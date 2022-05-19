package org.cafienne.querydb.materializer.slick

import akka.Done
import org.cafienne.cmmn.actorapi.command.platform.NewUserInformation
import org.cafienne.infrastructure.cqrs.offset.OffsetRecord
import org.cafienne.infrastructure.jdbc.cqrs.OffsetStoreTables
import org.cafienne.querydb.materializer.QueryDBTransaction
import org.cafienne.querydb.schema.QueryDBSchema
import org.cafienne.querydb.schema.table.{CaseTables, ConsentGroupTables, TaskTables, TenantTables}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.concurrent.{ExecutionContext, Future}

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

  val dbStatements: mutable.ListBuffer[DBIO[_]] = ListBuffer[DBIO[_]]()

  def addStatement(action: dbConfig.profile.api.DBIO[_]): Unit = dbStatements += action

  override def upsert(record: OffsetRecord): Unit = addStatement(TableQuery[OffsetStoreTable].insertOrUpdate(record))

  def commit(): Future[Done] = {
    val transaction = dbStatements.toSeq
    // Clear statement buffer (the "transaction")
    dbStatements.clear()

    // Run the actions
    db.run(DBIO.sequence(transaction).transactionally).map { _ => Done }
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
