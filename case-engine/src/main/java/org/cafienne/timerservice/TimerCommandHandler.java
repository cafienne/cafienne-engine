package org.cafienne.timerservice;

import org.cafienne.akka.actor.command.exception.AuthorizationException;
import org.cafienne.akka.actor.event.ModelEvent;
import org.cafienne.akka.actor.handler.CommandHandler;
import org.cafienne.timerservice.akka.command.TimerServiceCommand;

/**
 * Overwriting default command handler to disable unnecessary security checks
 * Security checks run for tenant and so, but TimerService runs as singleton within JVM
 */
public class TimerCommandHandler extends CommandHandler<TimerServiceCommand, ModelEvent, TimerService> {
    protected TimerCommandHandler(TimerService service, TimerServiceCommand command) {
        super(service, command);
    }

    @Override
    protected AuthorizationException runSecurityChecks() {
        // Need to override default CommandHandler security checking - all timers from all tenants are allowed ...
        return null;
    }
}