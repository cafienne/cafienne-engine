package org.cafienne.actormodel.command;

/**
 * The first command that is sent to a ModelActor has to implement this interface such that the actor can
 * initialize itself with the required information.
 * This is required to enable the ModelActor class to do some basic authorization checks that must be done by
 * the platform and cannot be left to actor specific logic overwriting it.
 */
public interface BootstrapCommand {
    String tenant();
}
