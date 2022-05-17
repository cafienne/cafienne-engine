package org.cafienne.querydb.materializer.cases.team

import akka.Done
import com.typesafe.scalalogging.LazyLogging
import org.cafienne.cmmn.actorapi.event.team._
import org.cafienne.cmmn.actorapi.event.team.deprecated.DeprecatedCaseTeamEvent
import org.cafienne.querydb.materializer.cases.{CaseEventBatch, CaseEventMaterializer}

import scala.concurrent.{ExecutionContext, Future}

class CaseTeamProjection(override val batch: CaseEventBatch)(implicit val executionContext: ExecutionContext) extends CaseEventMaterializer with LazyLogging {
  private val memberProjection = new CaseTeamMemberProjection(dBTransaction)
  private val deprecatedEventsProjection = new DeprecatedCaseTeamEventProjection(dBTransaction)

  def handleCaseTeamEvent(event: CaseTeamEvent): Future[Done] = {
    // We handle 2 types of event: either the old ones (which carried all info in one shot) or the new ones, which are more particular
    event match {
      case event: CaseTeamMemberEvent[_] => memberProjection.handleEvent(event)
      case event: DeprecatedCaseTeamEvent => deprecatedEventsProjection.handleDeprecatedCaseTeamEvent(event)
      case _ => // Ignore other events
    }
    Future.successful(Done)
  }

  def prepareCommit(): Unit = {
    memberProjection.prepareCommit()
    deprecatedEventsProjection.prepareCommit()
  }
}
