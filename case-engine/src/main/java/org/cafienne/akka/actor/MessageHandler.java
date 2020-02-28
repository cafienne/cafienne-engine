package org.cafienne.akka.actor;

import akka.actor.ActorRef;
import org.cafienne.akka.actor.command.ModelCommand;
import org.cafienne.akka.actor.command.exception.InvalidCommandException;
import org.cafienne.akka.actor.event.ModelEvent;
import org.cafienne.akka.actor.identity.TenantUser;
import org.cafienne.akka.actor.event.EngineVersionChanged;
import org.cafienne.cmmn.akka.event.debug.DebugEvent;
import org.cafienne.cmmn.instance.casefile.ValueMap;
import org.cafienne.cmmn.instance.debug.DebugAppender;
import org.cafienne.cmmn.instance.debug.DebugExceptionAppender;
import org.cafienne.cmmn.instance.debug.DebugStringAppender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
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

    protected MessageHandler(A actor, M msg) {
        this.actor = actor;
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
     * Returns the TenantUser that has initiated this message. May return null (typically in the case of InvalidMessages).
     *
     * @return
     */
    abstract protected TenantUser getUser();

    /**
     * Adds an event generated while handling the incoming message
     *
     * @param event
     */
    public <EV extends E> EV addEvent(EV event) {
        return addModelEvent(events.size(), event);
    }

    private <ME extends ModelEvent> ME addModelEvent(int index, ME event) {
        events.add(index, event);
        event.updateState(actor);
        return event;
    }

    /**
     * Method that can be invoked to add a DebugEvent
     *
     * @param event
     */
    private void addDebugEvent(DebugEvent event) {
        if (actor.recoveryRunning()) {
            return;
        }
        events.add(event);
    }

    /**
     * Returns the actor ref of the sender of the original message.
     *
     * @return
     */
    protected ActorRef sender() {
        return sender;
    }

    /**
     * Returns the actor ref of the model actor.
     *
     * @return
     */
    protected ActorRef self() {
        return actor.self();
    }

    public <T extends DebugEvent> void createDebugInfo(Class<T> eventClass, DebugAppender<T> appender) {
        if (actor.debugMode()) {
            try {
                Constructor<?>[] constructors = eventClass.getConstructors();
                @SuppressWarnings("unchecked")
                Constructor<T> tConstructor = (Constructor<T>) Arrays.stream(constructors).filter(c ->
                        c.getParameterCount() == 1 && c.getParameterTypes()[0].isAssignableFrom(actor.getClass())
                ).findFirst().orElseThrow(() -> new NoSuchMethodException("Cannot find a proper constructor for event " + eventClass.getName()));
                T newEvent = tConstructor.newInstance(actor);
                this.debugEvent = newEvent;
                this.addDebugEvent(newEvent);
                appender.add(newEvent);
            } catch (InstantiationException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                // Awesome. Now what?
                logger.error("Cannot instantiate debug event of type " + eventClass.getName(), e);
            }
        }
    }

    public <T extends DebugEvent> void addDebugInfo(Class<T> eventClass, DebugAppender<T> appender) {
        if (actor.debugMode()) {
            DebugEvent current = this.debugEvent;
            // If current debug event is not set or if it is not of the correct class, we'll create a new one.
            if (current != null && eventClass.equals(current.getClass())) {
                // Reuse existing event
                appender.add((T) current);
            } else {
                createDebugInfo(eventClass, appender);
            }
        }
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
        if (!actor.debugMode() && !logger.isDebugEnabled()) {
            return;
        }

        // First ensure log message is only generated once.
        String logMessage = appender.debugInfo();

        // If running in debug mode, add an event.
        if (actor.debugMode()) {
            this.addDebugInfo(DebugEvent.class, e -> e.addMessage(logMessage));
        }

        // If Log4J is enabled in debug mode, we will also invoke the appender.
        if (logger.isDebugEnabled()) {
            logger.debug(logMessage);
        }
    }

    public void addDebugInfo(DebugExceptionAppender appender, Logger logger) {
        // First check whether at all we should add some message.
        if (!actor.debugMode() && !logger.isDebugEnabled()) {
            return;
        }

        // First ensure log message is only generated once.
        Throwable t = appender.exceptionInfo();

        // If running in debug mode, add an event.
        if (actor.debugMode()) {
            this.addDebugInfo(DebugEvent.class, e -> e.addMessage(t));
        }

        // If Log4J is enabled in debug mode, we will also invoke the appender.
        if (logger.isDebugEnabled()) {
            logger.debug(t.getMessage(), t);
        }
    }
}
