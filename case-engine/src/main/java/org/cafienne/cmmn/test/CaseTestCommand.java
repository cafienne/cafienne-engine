/* 
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.test;

import org.cafienne.actormodel.command.BootstrapCommand;
import org.cafienne.actormodel.command.response.CommandFailure;
import org.cafienne.actormodel.command.response.ModelResponse;
import org.cafienne.cmmn.actorapi.command.CaseCommand;
import org.cafienne.cmmn.actorapi.response.CaseResponse;
import org.cafienne.cmmn.instance.Case;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.Value;
import org.cafienne.json.ValueMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple wrapper class for AkkaCaseCommands. It tells the engine to execute the command, and also captures the response. It only works if there is a
 * single JVM executing the case instance, as it holds a direct reference to the case instance that can be used for assertions.
 */
@Manifest
public class CaseTestCommand extends CaseCommand implements ModelTestCommand<CaseCommand, CaseResponse>, BootstrapCommand {
    private final static Logger logger = LoggerFactory.getLogger(CaseTestCommand.class);

    private transient final TestScript testScript;
    private transient final CaseResponseValidator validator;
    private final CaseCommand actualCommand;
    private CaseResponse actualResponse;
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
    @Override
    public CaseCommand getActualCommand() {
        return actualCommand;
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
    public ModelResponse process(Case caseInstance) {
        // Just have the actual command do its processing
        ModelResponse response = actualCommand.process(caseInstance);

        // Take a final snap after command has been processed
        caseSnapshotString = caseInstance.stateToXMLString();

        return response;
    }

    /**
     * Returns the response given by the case for the command; can only be accessed after the response has been received.
     * @return
     */
    @Override
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
     * Handles the response to this command by validating the result.
     * @param response The response to this CaseTestCommand
     */
    @Override
    public void handleResponse(CaseResponse response) {
        // Store the actual response.
        this.actualResponse = response;

        // Wait for the CaseModified event to be published
        awaitCaseModifiedEvent(response);

        logger.debug("Validating response for test command " + getActionNumber() + ": " + this.getActualCommand());
        // Run the validators. Validators raise an exception for the test script to stop. Typically an assertion error.
        if (validator != null) {
            validator.validate(this);
        }
    }

    @Override
    public void handleFailure(CommandFailure failure) {
        // Store the actual response.
        this.actualFailure = failure;

        logger.debug("Validating response for test command " + getActionNumber() + ": " + this.getActualCommand());
        // Run the validators. Validators raise an exception for the test script to stop. Typically an assertion error.
        if (validator != null) {
            validator.validate(this);
        }
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

    @Override
    public String caseInstanceString() {
        return this.caseSnapshotString;
    }

    @Override
    public String tenant() {
        if (actualCommand instanceof BootstrapCommand) {
            return ((BootstrapCommand) actualCommand).tenant();
        }
        throw new RuntimeException("This is not a BootstrapCommand");
    }

    @Override
    public Value<?> toJson() {
        return actualCommand.toJson();
    }
}
