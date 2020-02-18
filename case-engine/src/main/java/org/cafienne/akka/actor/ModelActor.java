package org.cafienne.akka.actor;

import akka.actor.Cancellable;
import akka.actor.PoisonPill;
import akka.persistence.AbstractPersistentActor;
import akka.persistence.RecoveryCompleted;
import org.cafienne.akka.actor.command.BootstrapCommand;
import org.cafienne.akka.actor.command.ModelCommand;
import org.cafienne.akka.actor.command.exception.InvalidCommandException;
import org.cafienne.akka.actor.command.response.CommandFailure;
import org.cafienne.akka.actor.command.response.CommandFailureListener;
import org.cafienne.akka.actor.command.response.CommandResponseListener;
import org.cafienne.akka.actor.command.response.ModelResponse;
import org.cafienne.akka.actor.event.ModelEvent;
import org.cafienne.akka.actor.handler.*;
import org.cafienne.akka.actor.CaseSystem;
import org.cafienne.akka.actor.identity.TenantUser;
import org.cafienne.cmmn.akka.command.CaseCommand;
import org.cafienne.akka.actor.event.EngineVersionChanged;
import org.cafienne.cmmn.akka.event.debug.DebugEvent;
import org.cafienne.cmmn.instance.casefile.ValueMap;
import org.cafienne.cmmn.instance.debug.DebugAppender;
import org.cafienne.cmmn.instance.debug.DebugExceptionAppender;
import org.cafienne.cmmn.instance.debug.DebugStringAppender;
import org.cafienne.processtask.akka.command.ProcessCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public abstract class ModelActor<C extends ModelCommand, E extends ModelEvent> extends AbstractPersistentActor {

    private final static Logger logger = LoggerFactory.getLogger(ModelActor.class);
    /**
     * The tenant in which this model is ran by the engine.
     */
    private String tenant;
    /**
     * The identifier of the model. Is expected to be unique. However, in practice it is derived from the Actor's path.
     */
    private String id;
    /**
     * Base class of commands that this ModelActor can handle
     */
    private final Class<C> commandClass;
    /**
     * Base class of events that this ModelActor can generate
     */
    private final Class<E> eventClass;
    /**
     * Reference to current command being processed by the ModelActor.
     */
    protected C currentCommand;

    /**
     * Flag indicating whether the model actor runs in debug mode or not
     */
    protected boolean debugMode = false;

    /**
     * Registration of listeners that are interacting with (other) models through this case.
     */
    private final Map<String, Responder> responseListeners = new HashMap<>();

    /**
     * CaseScheduler is a lightweight manager to schedule asynchronous works for this Case instance.
     */
    private final CaseScheduler scheduler;
    /**
     * The moment of last modification of the case, i.e., the moment at which the last correctly handled command was completed
     */
    private Instant lastModified;

    /**
     * The version of the engine that this case currently uses; this defaults to what comes from the BuildInfo.
     * If a ModelActor is recovered by Akka, then the version will be overwritten in {@link ModelActor#setEngineVersion(ValueMap)}.
     * Whenever then a new incoming message is handled by the Case actor - one leading to events, i.e., state changes, then
     * the actor will insert a new event EngineVersionChanged.
     * For new Cases, the CaseDefinitionApplied event will generate the current version
     */
    private ValueMap engineVersion;

    protected ModelActor(Class<C> commandClass, Class<E> eventClass) {
        this.id = self().path().name();
        this.commandClass = commandClass;
        this.eventClass = eventClass;
        this.scheduler = new CaseScheduler(this);
    }

    public ValueMap getEngineVersion() {
        return this.engineVersion;
    }

    public void setEngineVersion(ValueMap version) {
        this.engineVersion = version;
    }

    /**
     * Returns the id of the parent of this model, i.e., the one that created this model
     * and maintains it's lifecycle. Should return null or an empty string if there is none.
     * @return
     */
    public abstract String getParentActorId();

    /**
     * Returns the id of the parent of this model, i.e., the one that created this model
     * and maintains it's lifecycle. Should return null or an empty string if there is none.
     * @return
     */
    public abstract String getRootActorId();

    /**
     * Returns the Guid of the model instance
     *
     * @return
     */
    public String getId() {
        return id;
    }

    @Override
    public String persistenceId() {
        return this.id;
    }

    /**
     * Returns true if the model actor runs in debug mode, false otherwise.
     * @return
     */
    public boolean debugMode() {
        return this.debugMode;
    }

    /**
     * Returns the user context of the current command (or event, when running in recovery mode)
     *
     * @return
     */
    public TenantUser getCurrentUser() {
        return handler.getUser();
    }

    @Override
    public final Receive createReceiveRecover() {
        return receiveBuilder().match(Object.class, this::handleRecoveryEvent).build();
    }

    /**
     * Returns the scheduler for handling async work (from ProcessTasks and TimerEvent executions).
     * This is just a simple wrapper/delegator for a 'real' scheduler, allowing more fine-grained control
     * over async work in case of restarting an actor due to an error.
     *
     * @return
     */
    public CaseScheduler getScheduler() {
        return this.scheduler;
    }

    /**
     * Basic lifecycle handling of events upon recovery of this ModelActor.
     * Be careful in overriding it.
     *
     * @param event
     */
    protected void handleRecoveryEvent(Object event) {
        if (tenant == null && event instanceof ModelEvent) {
            // Probably this is the very first event in this actor...
            tenant = ((ModelEvent) event).tenant;
        }

        if (eventClass.isAssignableFrom(event.getClass()) || event instanceof EngineVersionChanged) {
            handler = createRecoveryHandler((E) event);
            logger.debug("Recovery in " + getClass().getSimpleName() + " " + getId() + ": recovering " + event);
            handler.process();
            handler.complete();
        } else if (event instanceof DebugEvent) {
            // No recovery from debug events ...
        } else if (event instanceof ModelEvent) {
            // Weird: ModelEvents in recovery of other models??
            logger.warn("Received unexpected recovery event of type "+event.getClass().getName()+" in actor of type "+getClass().getName());
        } else if (event instanceof RecoveryCompleted) {
            logger.info("Recovery of " + getClass().getSimpleName() + " " + getId() + " completed");
        } else {
            logger.warn("Received unknown event of type " + event.getClass().getName() + " during recovery: " + event);
        }
    }

    @Override
    public final Receive createReceive() {
        return receiveBuilder().match(Object.class, this::handleIncomingMessage).build();
    }

    /**
     * SelfCleaner provides a mechanism to have the ModelActor remove itself from memory after a specific idle period.
     */
    private Cancellable selfCleaner = null;

    /**
     * Basic lifecycle handling for incoming messages in this ModelActor.
     *
     * @param msg
     */
    final protected void handleIncomingMessage(Object msg) {
        clearSelfCleaner();

        handler = createMessageHandler(msg);

        InvalidCommandException securityIssue = handler.runSecurityChecks();
        if (securityIssue == null) {
            // Only process if we did not find any security issues.
            handler.process();
        }
        handler.complete();

        enableSelfCleaner();
    }

    private void clearSelfCleaner() {
        // Receiving message should reset the self-cleaning timer
        if (selfCleaner != null) selfCleaner.cancel();
    }

    private void enableSelfCleaner() {
        // Now set the new selfCleaner
        long idlePeriod = CaseSystem.config().actor().idlePeriod();
        FiniteDuration duration = Duration.create(idlePeriod, TimeUnit.MILLISECONDS);
        selfCleaner = getScheduler().schedule(duration, () -> {
            logger.debug("Removing actor " + getClass().getSimpleName() + " " + getId() + " from memory, as it has been idle for " + (idlePeriod / 1000) + " seconds");
//            System.out.println("Removing actor " + getClass().getSimpleName() + " " + getId() + " from memory, as it has been idle for " + (idlePeriod / 1000) + " seconds");
            self().tell(PoisonPill.getInstance(), self());
        });
    }

    private MessageHandler createMessageHandler(Object msg) {
        if (commandClass.isAssignableFrom(msg.getClass())) {
            if (tenant == null) {
                if (msg instanceof BootstrapCommand) {
                    this.tenant = ((BootstrapCommand) msg).tenant();
                } else {
                    return new NotConfiguredHandler(this, msg);
                }
            }
            C command = (C) msg;
            command.setActor(this);
            CommandHandler c = createCommandHandler(command);
            return c;
        } else if (msg instanceof ModelResponse) {
            if (tenant == null) {
                // We cannot handle responses if we have not been properly initialized.
                return new NotConfiguredHandler(this, msg);
            }
            return createResponseHandler((ModelResponse) msg);
        } else {
            return createInvalidMessageHandler(msg);
        }
    }

    private MessageHandler handler;

    protected <T extends MessageHandler> T currentHandler() {
        return (T) handler;
    }

    /**
     * Returns the current command being processed by the ModelActor.
     */
    public C getCurrentCommand() {
        return currentCommand;
    }

    /**
     * Adds an event to the current message handler
     * @param event
     * @param <EV>
     * @return
     */
    public <EV extends E> EV addEvent(EV event) {
        return (EV) currentHandler().addEvent(event);
    }

    /**
     * ModelActors can override this method to create their own handler for the command.
     *
     * @param msg
     * @return
     */
    protected CommandHandler createCommandHandler(C msg) {
        return new CommandHandler(this, msg);
    }

    /**
     * This method must be implemented by CommandHandlers to handle the fact that state changes
     * have taken place while handling the command. The ModelActor will get a new last modified timestamp,
     * and the actor should add an event for that to the log, so that projections can define and commit
     * a transaction scope
     * @param lastModified
     * @return
     */
    public abstract E createLastModifiedEvent(Instant lastModified);

    protected ResponseHandler createResponseHandler(ModelResponse response) {
        return new ResponseHandler(this, response);
    }

    protected InvalidMessageHandler createInvalidMessageHandler(Object message) {
        return new InvalidMessageHandler(this, message);
    }

    protected RecoveryEventHandler createRecoveryHandler(E message) {
        return new RecoveryEventHandler(this, message);
    }

    public Responder getResponseListener(String msgId) {
        return responseListeners.get(msgId);
    }

    /**
     * askCase allows inter-case communication. One case (or, typically, a plan item's special logic) can ask another case to execute
     * a command, and when the response is received back from the other case, the handler is invoked with that response.
     *
     * @param command
     * @param left    Listener to handle response failures.
     * @param right   Optional listener to handle response success.
     */
    public void askCase(CaseCommand command, CommandFailureListener left, CommandResponseListener... right) {
        responseListeners.put(command.getMessageId(), new Responder(left, right));
        CaseSystem.router().tell(command, self());
    }

    public void askProcess(ProcessCommand command, CommandFailureListener left, CommandResponseListener... right) {
        responseListeners.put(command.getMessageId(), new Responder(left, right));
        CaseSystem.router().tell(command, self());
    }

    /**
     * Returns the tenant in which the model is running.
     *
     * @return
     */
    public final String getTenant() {
        return tenant;
    }

    private void handlePersistFailure(Throwable cause, Object event, long seqNr) {
        // This code is invoked when there is a problem in connecting to the database while persisting events.
        //  Can also happen when a serialization of an event to JSON fails. In that case, recovery of the case seems not to work,
        //  whereas if we break e.g. Cassandra connection, it properly recovers after having invoked context().stop(self()).
        //  Not sure right now what the reason is for this.
        logger.error("Failure in "+getClass().getSimpleName()+" " + getId() + " during persistence of event " + seqNr + " of type " + event.getClass().getName() + ". Stopping instance.", cause);
        sender().tell(new CommandFailure(getCurrentCommand(), new Exception("Handling the request resulted in a system failure. Check the server logs for more information.")), self());
        context().stop(self());
    }

    @Override
    public void onPersistFailure(Throwable cause, Object event, long seqNr) {
        handlePersistFailure(cause, event, seqNr);
    }

    @Override
    public void onPersistRejected(Throwable cause, Object event, long seqNr) {
        handlePersistFailure(cause, event, seqNr);
    }

    public <T extends DebugEvent> void addDebugInfo(Class<T> eventClass, DebugAppender<T> appender) {
        if (!recoveryRunning()) {
            currentHandler().addDebugInfo(eventClass, appender);
        }
    }

    /**
     * Add debug info to the case if debug is enabled.
     * If the case runs in debug mode (or if Log4J has debug enabled for this logger),
     * then the appender's debugInfo method will be invoked to store a string in the log.
     *
     * @param appender
     */
    public void addDebugInfo(DebugStringAppender appender) {
        if (!recoveryRunning()) {
            currentHandler().addDebugInfo(appender, getLogger());
        }
    }

    public void addDebugInfo(DebugExceptionAppender appender) {
        if (!recoveryRunning()) {
            currentHandler().addDebugInfo(appender, getLogger());
        }
    }

    /**
     * Returns the moment at which the last modification to the case was done. I.e., the moment at which a command was completed that resulted into
     * events needing to be persisted.
     *
     * @return
     */
    public Instant getLastModified() {
        return lastModified;
    }

    public void setLastModified(Instant lastModified) {
        this.lastModified = lastModified;
    }

    /**
     * Returns the logger of the model actor
     * @return
     */
    protected Logger getLogger() {
        return logger;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "[" + getId() + "]";
    }
}
