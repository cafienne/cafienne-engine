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

package com.casefabric.cmmn.test;

import com.casefabric.actormodel.command.BootstrapMessage;
import com.casefabric.actormodel.response.CommandFailure;
import com.casefabric.actormodel.response.ModelResponse;
import com.casefabric.cmmn.actorapi.command.CaseCommand;
import com.casefabric.cmmn.actorapi.response.CaseResponse;
import com.casefabric.cmmn.instance.Case;
import com.casefabric.cmmn.test.assertions.PublishedEventsAssertion;
import com.casefabric.infrastructure.serialization.Manifest;
import com.casefabric.json.ValueMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple wrapper class for CaseCommands. It tells the engine to execute the command, and also captures the response. It only works if there is a
 * single JVM executing the case instance, as it holds a direct reference to the case instance that can be used for assertions.
 */
@Manifest
public class CaseTestCommand extends CaseCommand implements BootstrapMessage {
    private final static Logger logger = LoggerFactory.getLogger(CaseTestCommand.class);

    private transient final TestScript testScript;
    private transient final CaseResponseValidator validator;
    private final CaseCommand actualCommand;
    private ModelResponse actualResponse;
    private CommandFailure actualFailure;

    CaseTestCommand(TestScript testScript, CaseCommand command, CaseResponseValidator validator) {
        super(command.getUser(), command.getCaseInstanceId());
        this.testScript = testScript;
        this.actualCommand = command;
        this.validator = validator;
    }

    public CaseTestCommand(ValueMap json) {
        super(json);
        this.testScript = null;
        this.validator = null;
        // OOPS: this should actually be done properly...
        this.actualCommand = null;
    }

    @Override
    public boolean isBootstrapMessage() {
        return actualCommand.isBootstrapMessage();
    }

    /**
     * Returns the number this CaseTestCommand has inside the {@link TestScript}
     *
     * @return
     */
    public int getActionNumber() {
        return testScript.getActionNumber();
    }

    /**
     * Returns the actual command that was (or is to be) executed by the case instance
     * @return
     */
    public <T extends CaseCommand> T getActualCommand() {
        return (T) actualCommand;
    }

    boolean isTestScriptCommand() {
        return actualCommand instanceof TestScriptCommand;
    }

    @Deprecated // CaseSnapShotString is taken to show the "old" XML. To be replaced with some json based snapshotting
    private String caseSnapshotString = "No case available";

    @Override
    public void validate(Case caseInstance) {
        actualCommand.setActor(getActor());
        // Take a snap before any exceptions in command validation may or may not occur
        caseSnapshotString = caseInstance.stateToXMLString();
        // Now invoke actual validation and store the result.
        actualCommand.validate(caseInstance);
        // Take another snap before any exceptions in command validation may or may not occur
        caseSnapshotString = caseInstance.stateToXMLString();
    }

    @Override
    public void processCaseCommand(Case caseInstance) {
        // Just have the actual command do its processing
        actualCommand.processCaseCommand(caseInstance);
        actualResponse = actualCommand.getResponse();
        setResponse(actualResponse);

        // Take a final snap after command has been processed
        caseSnapshotString = caseInstance.stateToXMLString();
    }

    /**
     * Returns the response given by the case for the command; can only be accessed after the response has been received.
     * @return
     */
    public <R extends CaseResponse> R getActualResponse() {
        return (R) actualResponse;
    }

    /**
     * Returns the failure given back by the case engine for the command.
     * @return
     */
    public CommandFailure getActualFailure() {
        return actualFailure;
    }

    /**
     * Returns the list of events that were received after the command has been handled by the case
     * @return
     */
    public CaseEventListener getEventListener() {
        return testScript.getEventListener();
    }

    public String getCommandDescription() {
        return "CaseTestCommand[" + getActualCommand().getCommandDescription() +"]";
    }

    @Override
    public String toString() {
        return "Test command "+getActionNumber()+" of type " + getActualCommand().getClass().getSimpleName()+" resulted in " + getEvents().getEvents().size()+" events";
    }

    /**
     * Returns the list of events published for this test command, as published since by the actor id
     * @return
     */
    public PublishedEventsAssertion<?> getEvents() {
        return getEventListener().getNewEvents().filter(getActorId());
    }

    /**
     * Handles the response to this command by validating the result.
     * @param response The response to this CaseTestCommand
     */
    public void handleResponse(CaseResponse response) {
        // Store the actual response.
        this.actualResponse = response;

        // Wait for the CaseModified event to be published
        awaitCaseModifiedEvent(response);

        // Run the validators.
        runValidation();
    }

    void runValidation() {
        // Run the validators. Validators raise an exception for the test script to stop. Typically an assertion error.
        if (validator != null) {
            logger.debug("Validating response for test command " + getActionNumber() + ": " + this.getActualCommand());
            validator.validate(this);
        } else {
            logger.debug("Did not find validations for test command " + getActionNumber() + ": " + this.getActualCommand());
        }
    }

    public void handleFailure(CommandFailure failure) {
        // Store the actual response.
        this.actualFailure = failure;

        // Run the validators.
        runValidation();
    }

    private void awaitCaseModifiedEvent(CaseResponse response) {
        // If we ping a case before it has a definition, there will not be a last modified.
        //  But also there will not be any events generated, so we can return immediately.
        if (response.getLastModified() == null) {
            return;
        }

        logger.debug("Awaiting events for test command " + getActionNumber() + ": " + this.getActualCommand());

        // Now wait for the event stream listener to have handled all the events.
        getEventListener().awaitCaseModifiedEvent(response.getLastModified());
    }

    public String caseInstanceString() {
        return this.caseSnapshotString;
    }

    public String tenant() {
        if (actualCommand.isBootstrapMessage()) {
            return actualCommand.asBootstrapMessage().tenant();
        }
        throw new RuntimeException("This is not a BootstrapCommand");
    }

    @Override
    public ValueMap rawJson() {
        return actualCommand.rawJson();
    }
}
