package org.cafienne.actormodel.exception

/**
  * This exception is typically raised during the {@link ModelCommand#validate(ModelActor)} method.
  * The actor can throw this exception if the user issuing the command does not have a tenant.
  */
class EmptyTenantException(val msg: String) extends AuthorizationException(msg)