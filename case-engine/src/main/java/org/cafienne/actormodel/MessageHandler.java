package org.cafienne.actormodel;

import org.cafienne.actormodel.event.DebugEvent;
import org.cafienne.actormodel.event.EngineVersionChanged;
import org.cafienne.actormodel.event.ModelEvent;
import org.cafienne.actormodel.response.CommandFailure;
import org.cafienne.actormodel.response.ModelResponse;
import org.cafienne.cmmn.instance.debug.DebugExceptionAppender;
import org.cafienne.cmmn.instance.debug.DebugJsonAppender;
import org.cafienne.cmmn.instance.debug.DebugStringAppender;
import org.cafienne.infrastructure.Cafienne;
import org.cafienne.infrastructure.CafienneVersion;
import org.cafienne.infrastructure.enginedeveloper.EngineDeveloperConsole;
import org.cafienne.json.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Generic MessageHandler for incoming messages in ModelActors
 * An incoming message may lead to state changes in the ModelActor, and these can be added as ModelEvents.
 * In that sense, the MessageHandler can be considered like a sort of a transaction, but the handler
 * is itself responsible for persisting events
 * Message handlers must support 2 phases of handling a message:
 * <ul>
 * <li>{@link MessageHandler#process()}</li>
 * <li>{@link MessageHandler#complete()}</li>
 * </ul>
 *
 */
public abstract class MessageHandler {
    /**
     * The ModelActor to which the message was sent
     */
    protected final ModelActor actor;
    /**
     * The message that was sent to the actor that is being handled by this handler.
     */
    protected final Object msg;

    private final static Logger logger = LoggerFactory.getLogger(MessageHandler.class);

    protected final EventBatch events;

    protected ModelResponse response = null;

    protected MessageHandler(ModelActor actor, Object msg) {
        this.actor = actor;
        this.msg = msg;
        this.events = new EventBatch(actor, this);
    }

    protected void setResponse(ModelResponse response) {
        this.response = response;
    }

    protected void checkEngineVersion() {
        // First check whether the engine version has changed or not; this may lead to an EngineVersionChanged event
        CafienneVersion currentEngineVersion = Cafienne.version();
        CafienneVersion actorVersion = actor.getEngineVersion();
        if (actorVersion != null && currentEngineVersion.differs(actor.getEngineVersion())) {
            logger.info(this + " changed engine version from\n" + actor.getEngineVersion()+ " to\n" + currentEngineVersion);
            addEvent(new EngineVersionChanged(actor, currentEngineVersion));
        }
    }

    /**
     * Flag indicating whether events generated while handling the message should be persisted.
     * In case of invalid messages or recovery or akka system messages it should not.
     * In case of commands or responses to commands it should.
     * @return
     */
    protected boolean hasPersistence() {
        return false;
    }

    protected void handlePersistFailure(Throwable cause, Object event, long seqNr) {}

    /**
     * Lifecycle method
     */
    protected void process() {
    }

    /**
     * Lifecycle method
     */
    protected void beforeCommit() {
    }

    /**
     * Lifecycle method
     */
    protected void complete() {
        // Handling the incoming message can result in 3 different scenarios that are dealt with below:
        // 1. The message resulted in an exception that needs to be returned to the client; Possibly the case must be restarted.
        // 2. The message did not result in state changes (e.g., when fetching discretionary items), and the response can be sent straight away
        // 3. The message resulted in state changes, so the new events need to be persisted, and after persistence the response is sent back to the client.
        if (hasFailures()) { // Means there is a response AND it is of type CommandFailure
            events.abortWith(msg, response);
        } else {
            // Follow regular procedure
            beforeCommit();
            events.commit(response);
        }
    }

    protected boolean hasFailures() {
        return response != null && response instanceof CommandFailure;
    }

//    abstract protected void complete();

    /**
     * Adds an event generated while handling the incoming message
     *
     * @param event
     */
    public <T extends ModelEvent> T addEvent(T event) {
        events.addEvent(event);
        addDebugInfo(() -> "Updating actor state for new event "+ event.getDescription(), logger);
        event.updateActorState(actor);
        return event;
    }

    /**
     * Get or create the debug event for this message handler.
     * Only one debug event per handler, containing all debug messages.
     * @return
     */
    private DebugEvent getDebugEvent() {
        return events.getDebugEvent();
    }

    protected void addDebugInfo(DebugStringAppender appender, Value<?> json, Logger logger) {
        addDebugInfo(appender, logger);
        addDebugInfo(json::cloneValueNode, logger);
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
                EngineDeveloperConsole.debugIndentedConsoleLogging(logMessage); // special dev indentation in console
                getDebugEvent().addMessage(logMessage); // when actor runs in debug mode also publish events
            }
        }
    }

    public void addDebugInfo(DebugJsonAppender appender, Logger logger) {
        if (logDebugMessages()) {
            Value<?> json = appender.info();
            logger.debug(json.toString());
            EngineDeveloperConsole.debugIndentedConsoleLogging(json);
            getDebugEvent().addMessage(json);
        }
    }

    public void addDebugInfo(DebugExceptionAppender appender, Logger logger) {
        if (logDebugMessages()) {
            Throwable t = appender.exceptionInfo();
            logger.debug(t.getMessage(), t);
            EngineDeveloperConsole.debugIndentedConsoleLogging(t);
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
        return EngineDeveloperConsole.enabled() || actor.debugMode() || logger.isDebugEnabled();
    }
}
