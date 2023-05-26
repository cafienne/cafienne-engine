package org.cafienne.actormodel.command

import org.cafienne.infrastructure.serialization.JacksonSerializable

case class TerminateModelActor(actorId: String) extends JacksonSerializable
