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

import akka.actor.PoisonPill;
import akka.persistence.AbstractPersistentActor;
import akka.persistence.JournalProtocol;
import akka.persistence.SnapshotOffer;
import akka.persistence.SnapshotProtocol;
import org.cafienne.actormodel.command.BootstrapMessage;
import org.cafienne.actormodel.command.ModelCommand;
import org.cafienne.actormodel.event.ModelEvent;
import org.cafienne.actormodel.exception.CommandException;
import org.cafienne.actormodel.identity.UserIdentity;
import org.cafienne.actormodel.message.IncomingActorMessage;
import org.cafienne.actormodel.response.CommandFailure;
import org.cafienne.actormodel.response.CommandFailureListener;
import org.cafienne.actormodel.response.CommandResponseListener;
import org.cafienne.actormodel.response.ModelResponse;
import org.cafienne.cmmn.actorapi.command.CaseCommand;
import org.cafienne.cmmn.instance.debug.DebugInfoAppender;
import org.cafienne.infrastructure.Cafienne;
import org.cafienne.infrastructure.CafienneVersion;
import org.cafienne.infrastructure.enginedeveloper.EngineDeveloperConsole;
import org.cafienne.processtask.actorapi.command.ProcessCommand;
import org.cafienne.system.CaseSystem;
import org.cafienne.system.health.HealthMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public abstract class ModelActor extends AbstractPersistentActor {

    private final static Logger logger = LoggerFactory.getLogger(ModelActor.class);
    /**
     * The tenant in which this model is ran by the engine.
     */
    private String tenant;
    /**
     * The identifier of the model. Is expected to be unique. However, in practice it is derived from the Actor's path.
     */
    private final String id;
    /**
     * Front door knows ModelActor state, and determines whether visitors can pass.
     */
    private final Reception reception = new Reception(this);
    /**
     * Storage area for the ModelActor, keeps track of state changes
     */
    private final Warehouse warehouse = reception.warehouse;
    /**
     * User context of current message
     */
    private UserIdentity currentUser;
    /**
     * Flag indicating whether the model actor runs in debug mode or not
     */
    private boolean debugMode = Cafienne.config().actor().debugEnabled();

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

    /**
     * The CaseSystem in which this ModelActor runs
     */
    public final CaseSystem caseSystem;

    protected ModelActor(CaseSystem caseSystem) {
        this.caseSystem = caseSystem;
        this.id = self().path().name();
        this.scheduler = new CaseScheduler(this);
    }

    abstract protected boolean supportsCommand(Object msg);

    abstract protected boolean supportsEvent(ModelEvent msg);

    protected boolean hasAutoShutdown() {
        return true;
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
    public String getParentActorId() {
        return "";
    }

    /**
     * Returns the id of the parent of this model, i.e., the one that created this model
     * and maintains it's lifecycle. Should return null or an empty string if there is none.
     *
     * @return
     */
    public String getRootActorId() {
        return getId();
    }

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
    public UserIdentity getCurrentUser() {
        return currentUser;
    }

    public final void setCurrentUser(UserIdentity user) {
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
        return receiveBuilder().match(Object.class, reception::handleRecovery).build();
    }

    @Override
    public final Receive createReceive() {
        return receiveBuilder().match(Object.class, reception::handleMessage).build();
    }

    /**
     * Hook for handling snapshots.
     *
     * @param snapshot
     */
    protected void handleSnapshot(SnapshotOffer snapshot) {
    }

    protected void handleSnapshotProtocolMessage(SnapshotProtocol.Message message) {
    }

    protected void handleJournalProtocolMessage(JournalProtocol.Message message) {
//        System.out.println(this + ": Received " + message.getClass().getName());
    }

    protected void recoveryCompleted() {
        getLogger().info("Recovery of " + getClass().getSimpleName() + " " + getId() + " completed");
    }

    void takeABreak() {
        takeABreak("Removing actor " + getClass().getSimpleName() + " " + getId() + " from memory, as it has been idle for " + (Cafienne.config().actor().idlePeriod() / 1000) + " seconds");
    }

    void takeABreak(String msg) {
        getLogger().debug(msg);
//        System.out.println(msg);
        self().tell(PoisonPill.getInstance(), self());
    }

    protected void handleBootstrapMessage(BootstrapMessage message) {
        this.tenant = message.tenant();
    }

    /**
     * Adds an event to the current message handling context
     *
     * @param event
     * @param <E>
     * @return
     */
    public <E extends ModelEvent> E addEvent(E event) {
        warehouse.storeEvent(event);
        return event;
    }

    public Responder getResponseListener(String msgId) {
        synchronized (responseListeners) {
            return responseListeners.remove(msgId);
        }
    }

    public void informImplementation(ModelCommand command, CommandFailureListener left, CommandResponseListener... right) {
        askModel(command, left, right);
    }

    public void informParent(ModelCommand command, CommandFailureListener left, CommandResponseListener... right) {
        askModel(command, left, right);
    }

    /**
     * askModel allows communication between ModelActors. One case (or, typically, a plan item's special logic) can ask another case to execute
     * a command, and when the response is received back from the other case, the handler is invoked with that response.
     * Note that nothing will be sent to the other actor when recovery is running.
     *
     * @param command The message to send
     * @param left    Listener to handle response failures.
     * @param right   Optional listener to handle response success.
     */
    public void askModel(ModelCommand command, CommandFailureListener left, CommandResponseListener... right) {
        if (recoveryRunning()) {
//            System.out.println("Ignoring request to send command of type " + command.getClass().getName()+" because recovery is running");
            return;
        }
        synchronized (responseListeners) {
            responseListeners.put(command.getMessageId(), new Responder(left, right));
        }
        caseSystem.gateway().inform(command, self());
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
     * Model actor can send a reply to a command with this method
     *
     * @param response
     */
    public void reply(ModelResponse response) {
        // Always reset the transaction timestamp before replying. Even if there is no reply.
        resetTransactionTimestamp();
        if (response == null) {
            // Double check there is a response.
            return;
        }

        if (!(response instanceof CommandFailure)) {
            // Having handled a ModelCommand and properly stored the events we can unlock the reception.
            reception.unlock();
        }
        if (getLogger().isDebugEnabled() || EngineDeveloperConsole.enabled()) {
            String msg = "Sending response of type " + response.getClass().getSimpleName() + " from " + this;
            if (response instanceof CommandFailure) {
                msg += ": " + response;
            }

            getLogger().debug(msg);
            EngineDeveloperConsole.debugIndentedConsoleLogging(msg);
        }
        response.setLastModified(getLastModified());
        sender().tell(response, self());
    }

    /**
     * If the command handler has changed ModelActor state, but then ran into an unhandled exception,
     * the actor will remove itself from memory and start again.
     */
    public void failedWithInvalidState(Object msg, Throwable exception) {
        // Remove all schedules.
        this.getScheduler().clearSchedules();
        // Print errors
        if (exception instanceof CommandException) {
            getLogger().error("Restarting " + this + ". Handling msg of type " + msg.getClass().getName() + " resulted in invalid state.");
            getLogger().error("  Cause: " + exception.getClass().getSimpleName() + " - " + exception.getMessage());
        } else {
            getLogger().error("Encountered failure in handling msg of type " + msg.getClass().getName() + "; restarting " + this, exception);
        }
        // Tell our supervisor to restart us in order to clear any invalid state
        this.supervisorStrategy().restartChild(self(), exception, true);
    }

    private void handlePersistFailure(Throwable cause, Object event, long seqNr) {
        // This code is invoked when there is a problem in connecting to the database while persisting events.
        //  Can also happen when a serialization of an event to JSON fails. In that case, recovery of the case seems not to work,
        //  whereas if we break e.g. Cassandra connection, it properly recovers after having invoked context().stop(self()).
        //  Not sure right now what the reason is for this.

        // First log a message
        getLogger().error("Failure in " + getClass().getSimpleName() + " " + getId() + " during persistence of event " + seqNr + " of type " + event.getClass().getName() + ". Stopping instance.", cause);
        // Inform the HealthMonitor
        HealthMonitor.writeJournal().hasFailed(cause);
        // Optionally send a reply (in the CommandHandler). If persistence fails, also sending a reply may fail, hence first logging the issue.
        warehouse.handlePersistFailure(cause, event, seqNr);
        // Stop the actor
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
     * @param appender Producer of the log info
     * @param additionalInfo Additional parameters such as Throwable or json Value will be printed in a special manner
     */
    public void addDebugInfo(DebugInfoAppender appender, Object... additionalInfo) {
        addDebugInfo(getLogger(), appender, additionalInfo);
    }

    /**
     * Add debug info to the ModelActor if debug is enabled.
     * If the actor runs in debug mode (or if slf4j has debug enabled for this logger),
     * then the appender's debugInfo method will be invoked to store a string in the log.
     *
     * @param logger The slf4j logger instance to check whether debug logging is enabled
     * @param appender A functional interface returning "an" object, holding the main info to be logged.
     *                 Note: the interface is only invoked if logging is enabled. This appender typically
     *                 returns a String that is only created upon demand (in order to speed up a bit)
     * @param additionalInfo Additional objects to be logged. Typically, pointers to existing objects.
     */
    public void addDebugInfo(Logger logger, DebugInfoAppender appender, Object... additionalInfo) {
        warehouse.addDebugInfo(logger, appender, additionalInfo);
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
        return this.getClass().getSimpleName() + "[" + self().path().name() + "]";
    }

    /**
     * This method is invoked when handling of the source message completed and
     * resulting state changes are to be persisted in the event journal.
     * It can be used by e.g. ModelCommands and ModelResponses to add a {@link org.cafienne.actormodel.event.ActorModified} event.
     */
    protected void completeTransaction(IncomingActorMessage source) {
    }
}
