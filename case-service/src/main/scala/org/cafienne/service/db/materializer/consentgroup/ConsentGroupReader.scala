package org.cafienne.service.db.materializer.consentgroup

import org.cafienne.consentgroup.actorapi.event.ConsentGroupEvent
import org.cafienne.service.db.materializer.LastModifiedRegistration

object ConsentGroupReader {
  val lastModifiedRegistration: LastModifiedRegistration = new LastModifiedRegistration(ConsentGroupEvent.TAG)
}
