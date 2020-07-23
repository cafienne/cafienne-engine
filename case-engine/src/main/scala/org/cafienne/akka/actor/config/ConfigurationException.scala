package org.cafienne.akka.actor.config

case class ConfigurationException(msg: String) extends RuntimeException(msg)