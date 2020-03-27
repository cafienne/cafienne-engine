package org.cafienne.akka.actor;

import akka.actor.ActorRef;
import org.cafienne.akka.actor.command.ModelCommand;
import org.cafienne.akka.actor.command.exception.InvalidCommandException;
import org.cafienne.akka.actor.event.EngineVersionChanged;
import org.cafienne.akka.actor.event.ModelEvent;
import org.cafienne.akka.actor.identity.TenantUser;
import org.cafienne.akka.actor.event.DebugEvent;
import org.cafienne.cmmn.instance.casefile.Value;
import org.cafienne.cmmn.instance.debug.DebugExceptionAppender;
import org.cafienne.cmmn.instance.debug.DebugJsonAppender;
import org.cafienne.cmmn.instance.debug.DebugStringAppender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Generic MessageHandler for incoming messages in ModelActors.
 * Message handlers must support 2 phases of handling a message:
 * <ul>
 * <li>{@link MessageHandler#process()}</li>
 * <li>{@link MessageHandler#complete()}</li>
 * </ul>
 * There are 3 types of message handler. Using a generic M to abstract the differences.
 *
 * @param <M>
 */
public abstract class MessageHandler<M, C extends ModelCommand, E extends ModelEvent, A extends ModelActor<C, E>> {
    /**
     * The sender of the message; copied from the Actor upon creating the handler.
     */
    private final ActorRef sender;
    /**
     * The ModelActor to which the message was sent
     */
    protected final A actor;
    /**
     * The message that was sent to the actor that is being handled by this handler.
     */
    protected final M msg;

    private final static Logger logger = LoggerFactory.getLogger(MessageHandler.class);

    private final static int avgNumEvents = 30;

    protected final List<ModelEvent> events = new ArrayList<>(avgNumEvents);

    private DebugEvent debugEvent;

    protected MessageHandler(A actor, M msg, TenantUser user) {
        this.actor = actor;
        this.actor.setCurrentUser(user);
        // Take a copy of the sender at the moment we start handling the message (for lifecycle safety).
        this.sender = actor.sender();
        this.msg = msg;
    }

    /**
     * Lifecycle method
     * Returns null if there are no security issues, or an exception if some are found
     *
     * @return
     */
    abstract protected InvalidCommandException runSecurityChecks();

    protected void checkEngineVersion() {
        // First check whether the engine version has changed or not; this may lead to an EngineVersionChanged event
        CafienneVersion currentEngineVersion = CaseSystem.version();
        if (currentEngineVersion.differs(actor.getEngineVersion())) {
            logger.info(this + " changed engine version from\n" + actor.getEngineVersion()+ " to\n" + currentEngineVersion);
            addModelEvent(0, new EngineVersionChanged(actor, currentEngineVersion));
        }
    }

    /**
     * Lifecycle method
     */
    abstract protected void process();

    /**
     * Lifecycle method
     */
    abstract protected void complete();

    /**
     * Adds an event generated while handling the incoming message
     *
     * @param event
     */
    public <EV extends E> EV addEvent(EV event) {
        return addModelEvent(events.size(), event);
    }

    private EventBehaviorRunner currentBehavior = new EventBehaviorRunner(this);
    private boolean addingEvent = false;

    private <ME extends ModelEvent> ME addModelEvent(int index, ME event) {
        events.add(index, event);

        addDebugInfo(() -> "Updating actor state for new event "+ event.getDescription(), logger);
        if (addingEvent) {
            // TODO: This is a print statement to show where the engine still runs old style
            if (indentedConsoleLoggingEnabled && !(event instanceof DebugEvent)) {
//                addDebugInfo(() -> new Exception("Adding event while updating state?!"), logger);
                addDebugInfo(() -> "Adding event while updating state?!", logger);
            }
        }
        addingEvent = true;
        event.updateState(actor);
        addingEvent = false;
        // Now run the behavior that comes with the event, if any
        currentBehavior.pushEvent(event);
        return event;
    }

    /**
     * Get or create the debug event for this message handler.
     * Only one debug event per handler, containing all debug messages.
     * @return
     */
    private DebugEvent getDebugEvent() {
        if (debugEvent == null) {
            debugEvent = new DebugEvent(this.actor);
            if (actor.debugMode() && !actor.recoveryRunning()) {
                events.add(debugEvent);
            }
        }
        return debugEvent;
    }

    protected void addDebugInfo(DebugStringAppender appender, Value json, Logger logger) {
        addDebugInfo(appender, logger);
        addDebugInfo(() -> json, logger);
    }

    protected void addDebugInfo(DebugStringAppender appender, Exception exception, Logger logger) {
        addDebugInfo(appender, logger);
        addDebugInfo(() -> exception, logger);
    }

    /**
     * Add debug info to the case if debug is enabled.
     * If the case runs in debug mode (or if Log4J has debug enabled for this logger),
     * then the appender's debugInfo method will be invoked to store a string in the log.
     *
     * @param appender
     */
    public void addDebugInfo(DebugStringAppender appender, Logger logger) {
        // First check whether at all we should add some message.
        if (logDebugMessages()) {
            // Ensure log message is only generated once.
            String logMessage = appender.debugInfo();
            logger.debug(logMessage); // plain log4j
            debugIndentedConsoleLogging(logMessage); // special dev indentation in console
            getDebugEvent().addMessage(logMessage); // when actor runs in debug mode also publish events
        }
    }

    public void addDebugInfo(DebugJsonAppender appender, Logger logger) {
        if (logDebugMessages()) {
            Value json = appender.info();
            logger.debug(json.toString());
            debugIndentedConsoleLogging(json);
            getDebugEvent().addMessage(json);
        }
    }

    public void addDebugInfo(DebugExceptionAppender appender, Logger logger) {
        if (logDebugMessages()) {
            Throwable t = appender.exceptionInfo();
            logger.debug(t.getMessage(), t);
            debugIndentedConsoleLogging(t);
            getDebugEvent().addMessage(t);
        }
    }

    /**
     * Whether or not to write log messages.
     * Log messages are created through invocation of FunctionalInterface, and if this
     * method returns false, those interfaces are not invoked, in an attempt to improve runtime performance.
     * @return
     */
    private boolean logDebugMessages() {
        return indentedConsoleLoggingEnabled || actor.debugMode() || logger.isDebugEnabled();
    }

    /**
     * Making package private so that EventBehaviorRunner can read it
     */
    final boolean indentedConsoleLoggingEnabled = false;

    /**
     * Internal framework method only.
     * Making package private so that EventBehaviorRunner can read it
     */
    final void debugIndentedConsoleLogging(Object object) {
        if (! indentedConsoleLoggingEnabled) return;

        if (object instanceof Throwable) {
            // Print exception with newline before and after it. Will not be indented
            System.out.println("\n");
            ((Throwable) object).printStackTrace(System.out);
            System.out.println("\n");
        } else {
            // Convert Value to String if required
            String logMessage = String.valueOf(object);
            // Now get current level of indent from current behavior (recursive method)
            String indent = currentBehavior == null ? "" : currentBehavior.getIndent();
            // Make sure if it is a message with newlines, the new lines also get indented
            logMessage = logMessage.replaceAll("\n", "\n" + indent);
            // Print to console.
            System.out.println(indent + logMessage);
        }
    }
}
