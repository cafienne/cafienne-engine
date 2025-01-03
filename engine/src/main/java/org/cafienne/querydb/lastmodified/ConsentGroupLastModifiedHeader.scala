package org.cafienne.querydb.lastmodified

import org.cafienne.querydb.materializer.consentgroup.ConsentGroupReader

case class ConsentGroupLastModifiedHeader(override val value: Option[String]) extends LastModifiedHeader {
  override val name: String = Headers.CONSENT_GROUP_LAST_MODIFIED
  override val registration: LastModifiedRegistration = ConsentGroupReader.lastModifiedRegistration
}
