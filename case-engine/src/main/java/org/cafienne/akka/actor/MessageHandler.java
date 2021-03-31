package org.cafienne.akka.actor;

import org.cafienne.akka.actor.command.ModelCommand;
import org.cafienne.akka.actor.command.exception.AuthorizationException;
import org.cafienne.akka.actor.event.DebugEvent;
import org.cafienne.akka.actor.event.EngineVersionChanged;
import org.cafienne.akka.actor.event.ModelEvent;
import org.cafienne.akka.actor.identity.TenantUser;
import org.cafienne.akka.actor.serialization.json.Value;
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
     * The ModelActor to which the message was sent
     */
    protected final A actor;
    /**
     * The message that was sent to the actor that is being handled by this handler.
     */
    protected final M msg;

    protected final TenantUser user;

    private final static Logger logger = LoggerFactory.getLogger(MessageHandler.class);

    private final static int avgNumEvents = 30;

    protected final List<ModelEvent> events = new ArrayList<>(avgNumEvents);

    private DebugEvent debugEvent;

    protected MessageHandler(A actor, M msg, TenantUser user) {
        this.actor = actor;
        this.actor.setCurrentUser(user);
        this.msg = msg;
        this.user = user;
    }

    /**
     * Lifecycle method
     * Returns null if there are no security issues, or an exception if some are found
     *
     * @return
     */
    protected AuthorizationException runSecurityChecks() {
        return null;
    }

    protected void checkEngineVersion() {
        // First check whether the engine version has changed or not; this may lead to an EngineVersionChanged event
        CafienneVersion currentEngineVersion = CaseSystem.version();
        CafienneVersion actorVersion = actor.getEngineVersion();
        if (actorVersion != null && currentEngineVersion.differs(actor.getEngineVersion())) {
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
        return addModelEvent(event);
    }

    private EventBehaviorCallStack behaviorCallStack = new EventBehaviorCallStack();
    private boolean addingEvent = false;

    protected <ME extends ModelEvent> ME addModelEvent(ME event) {
        return addModelEvent(events.size(), event);
    }

    protected <ME extends ModelEvent> ME addModelEvent(int index, ME event) {
        events.add(index, event);

        addDebugInfo(() -> "Updating actor state for new event "+ event.getDescription(), logger);
        if (addingEvent) {
            // TODO: This is a print statement to show where the engine still runs old style
            if (CaseSystem.devDebugLogger().enabled() && !(event instanceof DebugEvent)) {
//                addDebugInfo(() -> new Exception("Adding event while updating state?!"), logger);
                addDebugInfo(() -> "Adding event while updating state?!", logger);
            }
        }
        addingEvent = true;
        event.updateState(actor);
        addingEvent = false;
        // Now run the behavior that comes with the event, if any
        behaviorCallStack.pushEvent(event);
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
        addDebugInfo(() -> json.cloneValueNode(), logger);
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
            if (! logMessage.isBlank()) { // Ignore blank messages
                logger.debug(logMessage); // plain log4j
                CaseSystem.devDebugLogger().debugIndentedConsoleLogging(logMessage); // special dev indentation in console
                getDebugEvent().addMessage(logMessage); // when actor runs in debug mode also publish events
            }
        }
    }

    public void addDebugInfo(DebugJsonAppender appender, Logger logger) {
        if (logDebugMessages()) {
            Value json = appender.info();
            logger.debug(json.toString());
            CaseSystem.devDebugLogger().debugIndentedConsoleLogging(json);
            getDebugEvent().addMessage(json);
        }
    }

    public void addDebugInfo(DebugExceptionAppender appender, Logger logger) {
        if (logDebugMessages()) {
            Throwable t = appender.exceptionInfo();
            logger.debug(t.getMessage(), t);
            CaseSystem.devDebugLogger().debugIndentedConsoleLogging(t);
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
        return CaseSystem.devDebugLogger().enabled() || actor.debugMode() || logger.isDebugEnabled();
    }

    /**
     * Returns true if the list of events generated has only debug events.
     * This is used in e.g. command handlers to determine whether or not the state of the actor has
     * actually changed during handling of the message. (DebugEvents are not supposed to change state...)
     * @return
     */
    protected boolean hasOnlyDebugEvents() {
        boolean hasOnlyDebugEvents = ! events.stream().anyMatch(e -> ! (e instanceof DebugEvent));
        return hasOnlyDebugEvents;
    }
}
