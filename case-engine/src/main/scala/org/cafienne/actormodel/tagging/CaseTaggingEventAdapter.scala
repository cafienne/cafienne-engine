/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.actormodel.tagging

import akka.persistence.journal.{Tagged, WriteEventAdapter}
import org.cafienne.actormodel.event.ModelEvent
import org.cafienne.cmmn.actorapi.event.plan.eventlistener.TimerBaseEvent
import org.cafienne.cmmn.actorapi.event.team.CaseTeamEvent
import org.cafienne.cmmn.actorapi.event.{CaseEvent, CaseModified}
import org.cafienne.humantask.actorapi.event.HumanTaskEvent
import org.cafienne.processtask.actorapi.event.ProcessInstanceEvent
import org.cafienne.tenant.actorapi.event.TenantEvent

class CaseTaggingEventAdapter extends WriteEventAdapter {
  override def manifest(event: Any): String = ""

  override def toJournal(event: Any): Any = event match {
    // Also CaseModified needs HumanTask tag, such that human task projections get a transaction scope too.
    case event: CaseModified => Tagged(event, Set(HumanTaskEvent.TAG, CaseEvent.TAG, ModelEvent.TAG))
    case event: TimerBaseEvent => Tagged(event, Set(TimerBaseEvent.TAG, CaseEvent.TAG, ModelEvent.TAG))
    case event: HumanTaskEvent => Tagged(event, Set(HumanTaskEvent.TAG, CaseEvent.TAG, ModelEvent.TAG))
    case event: CaseTeamEvent => Tagged(event, Set(HumanTaskEvent.TAG, CaseEvent.TAG, ModelEvent.TAG))
    case event: CaseEvent => Tagged(event, Set(CaseEvent.TAG, ModelEvent.TAG))
    case event: ProcessInstanceEvent => Tagged(event, Set(ProcessInstanceEvent.TAG, ModelEvent.TAG))
    case event: TenantEvent => Tagged(event, Set(TenantEvent.TAG, ModelEvent.TAG))
    case event: ModelEvent[_] => Tagged(event, Set(ModelEvent.TAG))
    case other => other
  }
}
