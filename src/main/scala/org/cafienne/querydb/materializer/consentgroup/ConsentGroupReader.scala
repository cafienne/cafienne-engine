package org.cafienne.querydb.materializer.consentgroup

import org.cafienne.consentgroup.actorapi.event.ConsentGroupEvent
import org.cafienne.querydb.materializer.LastModifiedRegistration

object ConsentGroupReader {
  val lastModifiedRegistration: LastModifiedRegistration = new LastModifiedRegistration(ConsentGroupEvent.TAG)
}
