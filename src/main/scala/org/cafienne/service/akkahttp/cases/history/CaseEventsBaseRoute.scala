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

package org.cafienne.service.akkahttp.cases.history

import akka.actor.ActorSystem
import akka.http.scaladsl.server.Route
import akka.stream.scaladsl.Sink
import org.cafienne.cmmn.actorapi.event.plan.CasePlanEvent
import org.cafienne.infrastructure.cqrs.{InstanceEventSource, ModelEventEnvelope}
import org.cafienne.querydb.query.exception.PlanItemSearchFailure
import org.cafienne.querydb.record.PlanItemHistoryRecord
import org.cafienne.service.akkahttp.cases.{CasesRoute, PlanItemHistory}

import scala.concurrent.Future

trait CaseEventsBaseRoute extends CasesRoute {

  def caseEventsSubRoute(subRoute: CaseEvents => Route): Route = {
    // TODO: consider requiring actual case ownership instead of case membership?
    //  (functional design question: who is allowed to read the full case event history)
    caseInstanceSubRoute { (user, caseInstanceId) =>
      authorizeCaseAccess(user, caseInstanceId, _ => subRoute(new CaseEvents(caseInstanceId)))
    }
  }


  class CaseEvents(val caseInstanceId: String) extends InstanceEventSource {
    override def system: ActorSystem = caseSystem.system

    /**
      * Returns the events as a sequence. Note: do not override this along with overriding the query to make it a livestream,
      * because then the seq never closes.
      */
    def eventList(): Future[Seq[ModelEventEnvelope]] = {
      // This code assumes checking for presence of events is done in the route on the query db tables.
      events(caseInstanceId).runWith(Sink.seq[ModelEventEnvelope])
    }

    def allCasePlanEvents(): Future[Seq[ModelEventEnvelope]] = {
      eventList().map(_.filter(_.event.isInstanceOf[CasePlanEvent]))
    }

    def planitemEvents(planItemId: String): Future[Seq[ModelEventEnvelope]] = {
      allCasePlanEvents().map(_.filter(_.event.asInstanceOf[CasePlanEvent].planItemId == planItemId))
    }

    def casePlanHistory(): Future[Seq[PlanItemHistory]] = {
      allCasePlanEvents().map(envelopes => {
        val planItemIdentifiers: Set[String] = envelopes.map(_.event.asInstanceOf[CasePlanEvent].planItemId).toSet
        planItemIdentifiers.map(id => {
          asHistory(id, envelopes.filter(_.event.asInstanceOf[CasePlanEvent].planItemId == id))
        }).toSeq
      })
    }

    def planitemHistory(planItemId: String): Future[PlanItemHistory] = {
      planitemEvents(planItemId).map(events => asHistory(planItemId, events))
    }

    def asHistory(planItemId: String, events: Seq[ModelEventEnvelope]): PlanItemHistory = {
      if (events.isEmpty) throw PlanItemSearchFailure(planItemId)
      val historyRecords: Seq[PlanItemHistoryRecord] = events.map(PlanItemHistoryMerger.mapModelEventEnvelope).filter(_.isDefined).map(_.get)
      var index = 0
      historyRecords.map(item => {
        val sequenced = item.copy(id = s"${planItemId}_$index", sequenceNr = index)
        index += 1
        sequenced
      })
      PlanItemHistory(historyRecords)
    }
  }
}
