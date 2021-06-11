package org.cafienne.infrastructure.akka.http

import akka.http.scaladsl.marshalling.Marshaller
import akka.http.scaladsl.model._
import org.cafienne.cmmn.actorapi.response.{CaseResponse, CaseResponseWithValueMap}
import org.cafienne.humantask.actorapi.response.HumanTaskResponse
import org.cafienne.tenant.actorapi.response.TenantOwnersResponse

/**
  * This file contains some marshallers and unmarshallers for responses from ModelActors
  */
object ResponseMarshallers {
  /**
    * Simple CaseResponse converter to JSON
    */
  implicit val caseResponseMarshaller = Marshaller.withFixedContentType(ContentTypes.`application/json`) { value: CaseResponse =>
    value match {
      case s: CaseResponseWithValueMap => {
        HttpEntity(ContentTypes.`application/json`, s.getResponse().toString)
      }
      case _ => {
        // TODO: extend this code to include case-last-modified header?!
        HttpEntity(ContentTypes.`application/json`, "{}")
      }
    }
  }

  /**
    * Simple CaseResponse converter to JSON
    */
  implicit val taskResponseMarshaller = Marshaller.withFixedContentType(ContentTypes.`application/json`) { value: HumanTaskResponse =>
    // TODO: extend this code to include case-last-modified header?!
    HttpEntity(ContentTypes.`application/json`, "{}")
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
