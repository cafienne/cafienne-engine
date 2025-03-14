package org.cafienne.actormodel;

import org.cafienne.actormodel.event.DebugEvent;
import org.cafienne.cmmn.instance.debug.DebugInfoAppender;
import org.cafienne.infrastructure.enginedeveloper.EngineDeveloperConsole;
import org.cafienne.json.Value;
import org.slf4j.Logger;

class TransactionLogger {
    private final ModelActorTransaction transaction;
    private final ModelActor actor;
    private DebugEvent debugEvent;

    TransactionLogger(ModelActorTransaction transaction, ModelActor actor) {
        this.transaction = transaction;
        this.actor = actor;
    }

    /**
     * Get or create the debug event for this batch.
     * Only one debug event per handler, containing all debug messages.
     */
    DebugEvent getDebugEvent() {
        if (debugEvent == null) {
            debugEvent = new DebugEvent(actor);
        }
        return debugEvent;
    }

    /**
     * Returns true if debug info is available, and also if the
     * actor runs in debug mode and is not in recovery
     */
    boolean hasDebugEvent() {
        return debugEvent != null && actor.debugMode() && !actor.recoveryRunning();
    }

    /**
     * Add debug info to the ModelActor if debug is enabled.
     * If the actor runs in debug mode (or if slf4j has debug enabled for this logger),
     * then the appender.debugInfo(...) method will be invoked to store a string in the log.
     *
     * @param logger         The slf4j logger instance to check whether debug logging is enabled
     * @param appender       A functional interface returning "an" object, holding the main info to be logged.
     *                       Note: the interface is only invoked if logging is enabled. This appender typically
     *                       returns a String that is only created upon demand (in order to speed up a bit)
     * @param additionalInfo Additional objects to be logged. Typically, pointers to existing objects.
     */
    void addDebugInfo(Logger logger, DebugInfoAppender appender, Object... additionalInfo) {
        if (logDebugMessages(logger)) {
            // Ensure log message is only generated once.
            Object info = appender.info();

            // Check whether the first additional parameter can be appended to the main info or deserves a separate
            //  logging entry. Typically, this is done for exceptions and json objects and arrays.
            if (additionalInfo.length == 0 || needsSeparateLine(additionalInfo[0])) {
                logObject(logger, info);
                logObjects(logger, additionalInfo);
            } else {
                additionalInfo[0] = String.valueOf(info) + additionalInfo[0];
                logObjects(logger, additionalInfo);
            }
        }
    }

    private boolean needsSeparateLine(Object additionalObject) {
        return additionalObject instanceof Throwable ||
                additionalObject instanceof Value && !((Value<?>) additionalObject).isPrimitive();
    }

    private void logObjects(Logger logger, Object... any) {
        for (Object o : any) {
            logObject(logger, o);
        }
    }

    private void logObject(Logger logger, Object o) {
        if (o instanceof Throwable t) {
            logException(logger, t);
        } else if (o instanceof Value<?> v) {
            logJSON(logger, v);
        } else {
            // Value of will also convert a null object to "null", so no need to check on null.
            logString(logger, String.valueOf(o));
        }
    }

    private void logString(Logger logger, String logMessage) {
        if (logMessage.isBlank()) {
            return;
        }
        logger.debug(logMessage); // plain slf4j
        EngineDeveloperConsole.debugIndentedConsoleLogging(logMessage); // special dev indentation in console
        getDebugEvent().addMessage(logMessage); // when actor runs in debug mode also publish events
    }

    private void logJSON(Logger logger, Value<?> json) {
        logger.debug(json.toString());
        EngineDeveloperConsole.debugIndentedConsoleLogging(json);
        getDebugEvent().addMessage(json);
    }

    private void logException(Logger logger, Throwable t) {
        logger.debug(t.getMessage(), t);
        EngineDeveloperConsole.debugIndentedConsoleLogging(t);
        getDebugEvent().addMessage(t);
    }

    /**
     * Whether to write log messages.
     * Log messages are created through invocation of FunctionalInterface, and if this
     * method returns false, those interfaces are not invoked, in an attempt to improve runtime performance.
     */
    private boolean logDebugMessages(Logger logger) {
        return EngineDeveloperConsole.enabled() || logger.isDebugEnabled() || actor.debugMode();
    }
}
