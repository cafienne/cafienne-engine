/*
 * Copyright (C) 2014  Batav B.V.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.cafienne.actormodel;

import org.cafienne.actormodel.debug.DebugInfoAppender;
import org.cafienne.actormodel.event.ModelEvent;
import org.cafienne.actormodel.message.IncomingActorMessage;
import org.cafienne.infrastructure.enginedeveloper.EngineDeveloperConsole;
import org.slf4j.Logger;

/**
 * Warehouse creates a new {@link ModelActorTransaction} for each {@link IncomingActorMessage}.
 */
class BackOffice {
    private final ModelActor actor;
    private final ModelActorMonitor monitor;
    private ModelActorTransaction currentTransaction;
    private boolean isOpen = false;

    BackOffice(ModelActor actor, ModelActorMonitor monitor) {
        this.actor = actor;
        this.monitor = monitor;
    }

    void performTransaction(IncomingActorMessage message) {
        // Tell the actor monitor we're busy
        monitor.setBusy();

        // We only receive incoming messages after the ModelActor has successfully recovered.
        //  This means that from now on, we must track events that are being added.
        isOpen = true;

        // Create a transaction context, and set it.
        //   The underlying Pekko system ensures that only one transaction is active at a time.
        currentTransaction = new ModelActorTransaction(actor, message);
        currentTransaction.perform();

        // Tell the actor monitor we're free again
        monitor.setFree();
    }

    void storeEvent(ModelEvent event) {
        if (isOpen) {
            currentTransaction.addEvent(event);
        } else {
            // Recovery is still running.

            // NOTE: ModelActors should not generate events during recovery.
            //  Such has been implemented for TenantActor and ProcessTaskActor, and partly for Case.
            //  Enabling the logging will showcase where this pattern has not been completely done.
            if (EngineDeveloperConsole.enabled()) {
                EngineDeveloperConsole.debugIndentedConsoleLogging("!!! Recovering " + actor + " generates event of type " + event.getClass().getSimpleName());
            }
        }
    }

    void handlePersistFailure(Throwable cause, Object event, long seqNr) {
        if (isOpen) {
            currentTransaction.handlePersistFailure(cause, event, seqNr);
        }
    }

    /**
     * Add debug info to the ModelActor if debug is enabled.
     * If the actor runs in debug mode (or if slf4j has debug enabled for this logger),
     * then the appender's debugInfo method will be invoked to store a string in the log.
     *
     * @param logger         The slf4j logger instance to check whether debug logging is enabled
     * @param appender       A functional interface returning "an" object, holding the main info to be logged.
     *                       Note: the interface is only invoked if logging is enabled. This appender typically
     *                       returns a String that is only created upon demand (in order to speed up a bit)
     * @param additionalInfo Additional objects to be logged. Typically, pointers to existing objects.
     */
    void addDebugInfo(Logger logger, DebugInfoAppender appender, Object... additionalInfo) {
        if (isOpen) {
            currentTransaction.addDebugInfo(logger, appender, additionalInfo);
        }
    }
}
