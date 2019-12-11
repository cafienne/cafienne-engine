package org.cafienne.akka.actor

case class OIDCConfig(connectUrl: String, tokenUrl: String, keysUrl: String, authorizationUrl: String, issuer: String)