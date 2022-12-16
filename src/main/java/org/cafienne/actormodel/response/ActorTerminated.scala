package org.cafienne.actormodel.response

import org.cafienne.storage.actormodel.message.StorageSerializable

case class ActorTerminated(actorId: String) extends StorageSerializable
