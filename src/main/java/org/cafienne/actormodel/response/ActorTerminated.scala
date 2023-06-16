package org.cafienne.actormodel.response

import org.cafienne.infrastructure.serialization.JacksonSerializable

case class ActorTerminated(actorId: String) extends JacksonSerializable
