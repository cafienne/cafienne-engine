package org.cafienne.infrastructure.akka.http

import akka.http.scaladsl.marshalling.Marshaller
import akka.http.scaladsl.model._
import org.cafienne.cmmn.akka.command.response.{CaseResponse, CaseResponseWithValueMap}
import org.cafienne.humantask.akka.command.response.HumanTaskResponse
import org.cafienne.tenant.akka.command.response.TenantOwnersResponse

/**
  * This file contains some marshallers and unmarshallers for responses from ModelActors
  */
object ResponseMarshallers {
  /**
    * Simple CaseResponse converter to JSON
    */
  implicit val caseResponseMarshaller = Marshaller.withFixedContentType(ContentTypes.`application/json`) { value: CaseResponse =>
    HttpEntity(ContentTypes.`application/json`, value.getResponse.toString)
  }

  /**
    * Simple CaseResponse converter to JSON
    */
  implicit val taskResponseMarshaller = Marshaller.withFixedContentType(ContentTypes.`application/json`) { value: HumanTaskResponse =>
    HttpEntity(ContentTypes.`application/json`, value.getResponse.toString)
  }

  /**
    * Simple response converter to JSON
    */
  implicit val tenantOwnersResponseMarshaller = Marshaller.withFixedContentType(ContentTypes.`application/json`) { o: TenantOwnersResponse =>
    val sb = new StringBuilder("[")
    val owners = o.owners
    var postfix = ""
    owners.forEach(o => {
      sb.append(postfix + "\"" + o + "\"")
      postfix = ", "
    })
    sb.append("]")

    HttpEntity(ContentTypes.`application/json`, sb.toString)
  }
}
