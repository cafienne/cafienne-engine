package org.cafienne.akka.actor.config.util

case class ConfigurationException(msg: String) extends RuntimeException(msg)
