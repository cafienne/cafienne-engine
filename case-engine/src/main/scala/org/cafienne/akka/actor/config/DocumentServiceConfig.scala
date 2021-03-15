package org.cafienne.akka.actor.config

import org.cafienne.akka.actor.config.util.MandatoryConfig
import org.cafienne.cmmn.instance.casefile.document.DocumentService

class DocumentServiceConfig(val parent: EngineConfig) extends MandatoryConfig {
  override def path = "document-service"

  /**
    * Document Service provides an interface for uploading and downloading case file documents
    */
  lazy val DocumentService: DocumentService = {
    val providerClassName = config.getString("provider")
    Class.forName(providerClassName).getDeclaredConstructor().newInstance().asInstanceOf[DocumentService]
  }

  lazy val location: String = {
    config.getString("location")
  }
}