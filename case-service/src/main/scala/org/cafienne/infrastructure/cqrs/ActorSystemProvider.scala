package org.cafienne.infrastructure.cqrs

import akka.actor.ActorSystem

trait ActorSystemProvider {
  implicit def system: ActorSystem
}
