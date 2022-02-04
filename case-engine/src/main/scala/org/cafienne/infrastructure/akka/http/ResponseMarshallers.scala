package org.cafienne.infrastructure.akka.http

import akka.http.scaladsl.marshalling.Marshaller
import akka.http.scaladsl.model._
import org.cafienne.actormodel.response.ModelResponse

/**
  * This file contains some marshallers and unmarshallers for responses from ModelActors
  */
object ResponseMarshallers {
  /**
    * Simple CaseResponse converter to JSON
    */
  implicit val modelResponseMarshaller = Marshaller.withFixedContentType(ContentTypes.`application/json`) { value: ModelResponse =>
    HttpEntity(ContentTypes.`application/json`, value.toJson.toString)
  }
}
