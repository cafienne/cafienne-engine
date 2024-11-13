package com.casefabric.querydb.lastmodified

import com.casefabric.actormodel.response.ActorLastModified

import scala.concurrent.Future

trait LastModifiedHeader {
  val name: String
  val registration: LastModifiedRegistration
  val value: Option[String] = None
  val lastModified: Option[ActorLastModified] = value.map(new ActorLastModified(name, _))

  override def toString: String = name + ": " + value

  def available: Future[String] = {
    if (lastModified.isDefined) {
      //    println("Awaiting " + this)
      registration.waitFor(lastModified.get).future
    } else {
      Future.successful("No header present")
    }
  }
}

object LastModifiedHeader {
  val NONE: LastModifiedHeader = new LastModifiedHeader {
    override val name: String = ""
    override val registration: LastModifiedRegistration = null
  }

  def get(headerName: String, headerValue: Option[String] = None): LastModifiedHeader = headerName match {
    case Headers.CASE_LAST_MODIFIED => CaseLastModifiedHeader(headerValue)
    case Headers.TENANT_LAST_MODIFIED => TenantLastModifiedHeader(headerValue)
    case Headers.CONSENT_GROUP_LAST_MODIFIED => ConsentGroupLastModifiedHeader(headerValue)
    case _ => throw new Exception(s"Unrecognized last modified header $headerName")
  }
}