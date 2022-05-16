package org.cafienne.querydb.materializer.consentgroup

import akka.Done
import com.typesafe.scalalogging.LazyLogging
import org.cafienne.consentgroup.actorapi.event.ConsentGroupCreated
import org.cafienne.querydb.record.ConsentGroupRecord

import scala.concurrent.Future

class GroupProjection(dBTransaction: ConsentGroupStorageTransaction) extends LazyLogging {
  def handleGroupEvent(event: ConsentGroupCreated): Future[Done] = {
    val groupRecord = ConsentGroupRecord(id = event.getActorId, tenant = event.tenant)
    dBTransaction.upsert(groupRecord)
    Future.successful(Done)
  }

  def prepareCommit(): Unit = {
    // Nothing to do here currently
  }
}
