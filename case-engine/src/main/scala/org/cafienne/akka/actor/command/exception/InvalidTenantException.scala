package org.cafienne.akka.actor.command.exception

import org.cafienne.akka.actor.ModelActor
import org.cafienne.akka.actor.identity.TenantUser

/**
  * This exception is typically raised during the {@link ModelCommand#validate(ModelActor)} method.
  * The actor can throw this exception if the user issuing the command is not authorized.
  */
class InvalidTenantException(val user: TenantUser, val msg: Any, val actor: ModelActor[_,_])
  extends AuthorizationException(s"User ${user.id} in tenant ${user.tenant} tries to run ${msg.getClass.getName} on $actor in tenant ${actor.getTenant}")