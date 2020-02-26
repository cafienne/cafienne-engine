package org.cafienne.infrastructure.akka.http

import akka.http.scaladsl.marshalling.Marshaller
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshaller
import org.cafienne.akka.actor.identity.TenantUser
import org.cafienne.cmmn.akka.command.response.{CaseResponse, CaseResponseWithValueMap}
import org.cafienne.cmmn.instance.casefile.{JSONReader, ValueList, ValueMap}
import org.cafienne.humantask.akka.command.response.HumanTaskResponse
import org.cafienne.tenant.akka.command.response.TenantOwnersResponse
import org.cafienne.util.XMLHelper
import org.w3c.dom.Document

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

  /**
    * Simple TenantUser converter to JSON
    * This does not spit out the enabled property of TenantUser
    */
  implicit val tenantUserMarshaller = Marshaller.withFixedContentType(ContentTypes.`application/json`) { user: TenantUser =>
      HttpEntity(ContentTypes.`application/json`, user.toJson.toString)
  }

  /**
    * Simple TenantUser list converter to JSON
    */
  implicit val tenantUsersMarshaller = Marshaller.withFixedContentType(ContentTypes.`application/json`) { users: Seq[TenantUser] =>
    val sb = new StringBuilder("[")
    var postfix = ""
    users.foreach(tenantUser => {
      sb.append(postfix + tenantUser.toJson.toString)
      postfix = ", "
    })
    sb.append("]")

    HttpEntity(ContentTypes.`application/json`, sb.toString)
  }
}
