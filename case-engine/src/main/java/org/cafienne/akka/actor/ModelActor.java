package org.cafienne.akka.actor;

import akka.actor.Cancellable;
import akka.actor.PoisonPill;
import akka.persistence.AbstractPersistentActor;
import akka.persistence.RecoveryCompleted;
import akka.persistence.SnapshotMetadata;
import org.cafienne.akka.actor.command.BootstrapCommand;
import org.cafienne.akka.actor.command.ModelCommand;
import org.cafienne.akka.actor.command.exception.AuthorizationException;
import org.cafienne.akka.actor.command.exception.InvalidCommandException;
import org.cafienne.akka.actor.command.response.CommandFailure;
import org.cafienne.akka.actor.command.response.CommandFailureListener;
import org.cafienne.akka.actor.command.response.CommandResponseListener;
import org.cafienne.akka.actor.command.response.ModelResponse;
import org.cafienne.akka.actor.event.EngineVersionChanged;
import org.cafienne.akka.actor.event.ModelEvent;
import org.cafienne.akka.actor.event.TransactionEvent;
import org.cafienne.akka.actor.handler.*;
import org.cafienne.akka.actor.identity.TenantUser;
import org.cafienne.cmmn.akka.command.CaseCommand;
import org.cafienne.akka.actor.event.DebugEvent;
import org.cafienne.cmmn.instance.casefile.Value;
import org.cafienne.cmmn.akka.event.file.CaseFileEvent;
import org.cafienne.cmmn.akka.event.plan.PlanItemEvent;
import org.cafienne.cmmn.instance.debug.DebugStringAppender;
import org.cafienne.processtask.akka.command.ProcessCommand;
import org.cafienne.timerservice.akka.command.TimerServiceCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
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
     * User context of current message
     */
    private TenantUser currentUser;

    /**
     * Flag indicating whether the model actor runs in debug mode or not
     */
    private boolean debugMode = CaseSystem.config().actor().debugEnabled();

    /**
     * Registration of listeners that are interacting with (other) models through this case.
     */
    private final Map<String, Responder> responseListeners = new HashMap();

    /**
     * CaseScheduler is a lightweight manager to schedule asynchronous works for this Case instance.
     */
    private final CaseScheduler scheduler;
    /**
     * The moment of last modification of the case, i.e., the moment at which the last correctly handled command was completed
     */
    private Instant lastModified;
    /**
     * The moment the next transaction is started; will be used to fill the LastModified event and also inbetween timestamps in events.
     */
    private Instant transactionTimestamp;


    /**
     * The version of the engine that this case currently uses; this defaults to what comes from the BuildInfo.
     * If a ModelActor is recovered by Akka, then the version will be overwritten in {@link ModelActor#setEngineVersion(CafienneVersion)}.
     * Whenever then a new incoming message is handled by the Case actor - one leading to events, i.e., state changes, then
     * the actor will insert a new event EngineVersionChanged.
     * For new Cases, the CaseDefinitionApplied event will generate the current version
     */
    private CafienneVersion engineVersion;

    protected ModelActor(Class<C> commandClass, Class<E> eventClass) {
        this.id = self().path().name();
        this.commandClass = commandClass;
        this.eventClass = eventClass;
        this.scheduler = new CaseScheduler(this);
    }

    public CafienneVersion getEngineVersion() {
        return this.engineVersion;
    }

    public void setEngineVersion(CafienneVersion version) {
        this.engineVersion = version;
    }

    /**
     * Returns the id of the parent of this model, i.e., the one that created this model
     * and maintains it's lifecycle. Should return null or an empty string if there is none.
     *
     * @return
     */
    public abstract String getParentActorId();

    /**
     * Returns the id of the parent of this model, i.e., the one that created this model
     * and maintains it's lifecycle. Should return null or an empty string if there is none.
     *
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
     * Switch debug mode of the ModelActor.
     * When debug mode is enabled, log messages will be added to a DebugEvent that is persisted upon handling
     * an incoming message or recovery.
     *
     * @param debugMode
     */
    public void setDebugMode(boolean debugMode) {
        this.debugMode = debugMode;
    }

    /**
     * Returns true if the model actor runs in debug mode, false otherwise.
     *
     * @return
     */
    public boolean debugMode() {
        return this.debugMode;
    }

    /**
     * Returns the user context of the current command, event or response
     *
     * @return
     */
    public TenantUser getCurrentUser() {
        return currentUser;
    }

    final void setCurrentUser(TenantUser user) {
        this.currentUser = user;
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

    @Override
    public Receive createReceiveRecover() {
        return receiveBuilder().match(Object.class, this::handleRecovery).build();
    }

    protected void handleRecovery(Object event) {
        // Steps:
        // 1. Set tenant if not yet available
        // 2. Check whether this is a valid type of ModelEvent for this type of ModelActor
        //    a. If so, run the recovery handler for it
        //    b. Ignore DebugEvents and RecoveryCompleted message
        //    c. In all other cases print warn statements, with a special check for other ModelEvents

        // Step 1
        if (tenant == null && event instanceof ModelEvent) {
            tenant = ((ModelEvent) event).getTenant();
        }
        // Step 2
        if (eventClass.isAssignableFrom(event.getClass()) || event instanceof EngineVersionChanged) {
            // Step 2a.
            runHandler(createRecoveryHandler((E) event));
        } else if (event instanceof DebugEvent) {
            // Step 2b.
            // No recovery from debug events ...
        } else if (event instanceof RecoveryCompleted) {
            // Step 2b.
            recoveryCompleted();
        } else if (event instanceof ModelEvent) {
            // Step 2c. Weird: ModelEvents in recovery of other models??
            logger.warn("Received unexpected recovery event of type " + event.getClass().getName() + " in actor of type " + getClass().getName());
        } else {
            // Step 2c.
            logger.warn("Received unknown event of type " + event.getClass().getName() + " during recovery: " + event);
        }
    }

    protected void recoveryCompleted() {
        logger.info("Recovery of " + getClass().getSimpleName() + " " + getId() + " completed");
    }

    @Override
    public final Receive createReceive() {
        return receiveBuilder().match(Object.class, msg -> {

//            System.out.println(this.getClass().getSimpleName() + ": Received a msg of type " + msg.getClass().getSimpleName());

            // Steps:
            // 1. Remove self cleaner
            // 2. Handle message
            // 3. Set a new self cleaner (basically resets the timer)
            clearSelfCleaner();
            runHandler(createMessageHandler(msg));
            enableSelfCleaner();
        }).build();
    }

    /**
     * SelfCleaner provides a mechanism to have the ModelActor remove itself from memory after a specific idle period.
     */
    private Cancellable selfCleaner = null;

    protected void clearSelfCleaner() {
        // Receiving message should reset the self-cleaning timer
        if (selfCleaner != null) selfCleaner.cancel();
    }

    protected void enableSelfCleaner() {
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
                    return new NotConfiguredHandler(this, (ModelCommand) msg);
                }
            }
            C command = (C) msg;
            command.setActor(this);
            CommandHandler c = createCommandHandler(command);
            return c;
        } else if (msg instanceof ModelResponse) {
            if (tenant == null) {
                // We cannot handle responses if we have not been properly initialized.
                return new NotConfiguredHandler(this, (ModelResponse) msg);
            }
            return createResponseHandler((ModelResponse) msg);
        } else if (msg.getClass().getPackage().equals(SnapshotMetadata.class.getPackage())) {
            return createAkkaSystemMessageHandler(msg);
        } else {
            return createInvalidMessageHandler(msg);
        }
    }

    /**
     * Execute the lifecycle in handling the incoming message:
     * - run security checks
     * - if no issues from there, invoke process method
     * - and finally invoke complete method.
     *
     * @param handler
     */
    private void runHandler(MessageHandler handler) {
        this.currentMessageHandler = handler;

        AuthorizationException securityIssue = this.currentMessageHandler.runSecurityChecks();
        if (securityIssue == null) {
            // Only process if we did not find any security issues.
            this.currentMessageHandler.process();
        }
        this.currentMessageHandler.complete();
    }

    private MessageHandler currentMessageHandler;

    /**
     * Returns a typed version of the current message handler
     *
     * @param <T>
     * @return
     */
    private <T extends MessageHandler> T currentHandler() {
        return (T) currentMessageHandler;
    }

    /**
     * Basic handler for commands received in this ModelActor.
     * Be careful in overriding it.
     *
     * @param command
     */
    protected CommandHandler createCommandHandler(C command) {
        return new CommandHandler(this, command);
    }

    /**
     * Basic handler for response messages received from other ModelActors in this ModelActor.
     * Be careful in overriding it.
     *
     * @param response
     */
    protected ResponseHandler createResponseHandler(ModelResponse response) {
        return new ResponseHandler(this, response);
    }

    /**
     * Basic handler for wrongly typed messages received in this ModelActor.
     * Be careful in overriding it.
     *
     * @param message
     */
    protected InvalidMessageHandler createInvalidMessageHandler(Object message) {
        return new InvalidMessageHandler(this, message);
    }

    /**
     * Handler for akka system messages (e.g. SnapshotOffer, SnapshotSaveSuccess, RecoveryCompleted, etc)
     * @param message
     * @return
     */
    protected AkkaSystemMessageHandler createAkkaSystemMessageHandler(Object message) {
        return new AkkaSystemMessageHandler(this, message);
    }

    /**
     * Basic handler of events upon recovery of this ModelActor.
     * Be careful in overriding it.
     *
     * @param event
     */
    protected RecoveryEventHandler createRecoveryHandler(E event) {
        return new RecoveryEventHandler(this, event);
    }

    /**
     * Returns the current command being processed by the ModelActor.
     */
    public C getCurrentCommand() {
        return currentCommand;
    }

    /**
     * Adds an event to the current message handler
     *
     * @param event
     * @param <EV>
     * @return
     */
    public <EV extends E> EV addEvent(EV event) {
        return (EV) currentHandler().addEvent(event);
    }

    /**
     * This method must be implemented by CommandHandlers to handle the fact that state changes
     * have taken place while handling the command. The ModelActor may return an event for that to the log,
     * so that projections can use that to handle a bulk of events and commit a transaction scope.
     * Note that the method may return null.
     *
     * @return
     */
    public abstract TransactionEvent createTransactionEvent();

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
        if (recoveryRunning()) {
            return;
        }
        responseListeners.put(command.getMessageId(), new Responder(left, right));
        CaseSystem.router().tell(command, self());
    }

    public void askProcess(ProcessCommand command, CommandFailureListener left, CommandResponseListener... right) {
        if (recoveryRunning()) {
            return;
        }
        responseListeners.put(command.getMessageId(), new Responder(left, right));
        CaseSystem.router().tell(command, self());
    }

    public void askTimerService(TimerServiceCommand command, CommandFailureListener left, CommandResponseListener... right) {
        if (recoveryRunning()) {
            return;
        }
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

    /**
     * Method for a MessageHandler to persist it's events
     *
     * @param events
     * @param <T>
     */
    public <T> void persistEvents(List<T> events) {
        persistEventsAndThenReply(events, null);
    }

    /**
     * Model actor can send a reply to a command with this method
     *
     * @param response
     */
    public void reply(ModelResponse response) {
        if (logger.isDebugEnabled() || currentMessageHandler.indentedConsoleLoggingEnabled) {
            String msg = "Sending response of type " + response.getClass().getSimpleName() + " from " + this;
            logger.debug(msg);
            currentMessageHandler.debugIndentedConsoleLogging(msg);
        }
        sender().tell(response, self());
    }

    /**
     * Method for a MessageHandler to persist it's events, and be called back after all events have been persisted
     *
     * @param events
     * @param response
     * @param <T>
     */
    public <T> void persistEventsAndThenReply(List<T> events, ModelResponse response) {
        if (logger.isDebugEnabled() || currentMessageHandler.indentedConsoleLoggingEnabled) {
            StringBuilder msg = new StringBuilder("\n------------------------ PERSISTING " + events.size() + " EVENTS IN " + this);
            events.forEach(e -> {
                msg.append("\n\t");
                if (e instanceof PlanItemEvent) {
                    msg.append(e.toString());
                } else if (e instanceof CaseFileEvent) {
                    msg.append(e.getClass().getSimpleName() + "." + ((CaseFileEvent) e).getTransition() + "()[" + ((CaseFileEvent) e).getPath() + "]");
                } else {
                    msg.append(e.getClass().getSimpleName() + ", ");
                }
            });
            logger.debug(msg + "\n");
            currentMessageHandler.debugIndentedConsoleLogging(msg + "\n");
        }
        if (events.isEmpty()) {
            return;
        }
        T lastEvent = events.get(events.size() - 1);
        persistAll(events, e -> {
            CaseSystem.health().writeJournal().isOK();
            if (logger.isDebugEnabled()) {
                logger.debug("Persisted an event of type " + e.getClass().getName() + " in actor " + this);
            }
            if (e == lastEvent && response != null) {
                reply(response);
            }
        });
    }

    /**
     * If the command handler has changed ModelActor state, but then ran into an unhandled exception,
     * the actor will remove itself from memory and start again.
     *
     * @param handler
     * @param exception
     */
    public void failedWithInvalidState(CommandHandler handler, Throwable exception) {
        this.getScheduler().clearSchedules(); // Remove all schedules.
        logger.error("Encountered failure in handling msg of type " + handler.msg.getClass().getName() + "; restarting " + this, exception);
        this.supervisorStrategy().restartChild(self(), exception, true);
    }

    private void handlePersistFailure(Throwable cause, Object event, long seqNr) {
        // This code is invoked when there is a problem in connecting to the database while persisting events.
        //  Can also happen when a serialization of an event to JSON fails. In that case, recovery of the case seems not to work,
        //  whereas if we break e.g. Cassandra connection, it properly recovers after having invoked context().stop(self()).
        //  Not sure right now what the reason is for this.
        CaseSystem.health().writeJournal().hasFailed(cause);
        logger.error("Failure in " + getClass().getSimpleName() + " " + getId() + " during persistence of event " + seqNr + " of type " + event.getClass().getName() + ". Stopping instance.", cause);
        reply(new CommandFailure(getCurrentCommand(), new Exception("Handling the request resulted in a system failure. Check the server logs for more information.")));
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

    /**
     * Add debug info to the case if debug is enabled.
     * If the case runs in debug mode (or if Log4J has debug enabled for this logger),
     * then the appender's debugInfo method will be invoked to store a string in the log.
     *
     * @param appender
     */
    public void addDebugInfo(DebugStringAppender appender) {
        currentHandler().addDebugInfo(appender, getLogger());
    }

    public void addDebugInfo(DebugStringAppender appender, Value json) {
        currentHandler().addDebugInfo(appender, json, getLogger());
    }

    public void addDebugInfo(DebugStringAppender appender, Exception exception) {
        currentHandler().addDebugInfo(appender, exception, getLogger());
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

    /**
     * Returns the moment at which the last modification to the case was done. I.e., the moment at which a command was completed that resulted into
     * events needing to be persisted.
     *
     * @return
     */
    public Instant getTransactionTimestamp() {
        if (transactionTimestamp == null) {
            transactionTimestamp = Instant.now();
        }
        return transactionTimestamp;
    }

    public void resetTransactionTimestamp() {
        transactionTimestamp = null;
    }

    public void setLastModified(Instant lastModified) {
        this.lastModified = lastModified;
    }

    /**
     * Returns the logger of the model actor
     *
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
