/* 
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.test.humantask;

import org.cafienne.akka.actor.command.exception.InvalidCommandException;
import org.cafienne.akka.actor.command.response.CommandFailure;
import org.cafienne.akka.actor.command.response.ModelResponse;
import org.cafienne.cmmn.instance.task.humantask.HumanTask;
import org.cafienne.cmmn.test.CaseEventListener;
import org.cafienne.cmmn.test.ModelTestCommand;
import org.cafienne.cmmn.test.TestScript;
import org.cafienne.humantask.akka.command.HumanTaskCommand;
import org.cafienne.humantask.akka.command.response.HumanTaskResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple wrapper class for AkkaCaseCommands. It tells the engine to execute the command, and also captures the response. It only works if there is a
 * single JVM executing the case instance, as it holds a direct reference to the case instance that can be used for assertions.
 */
public class HumanTaskTestCommand extends HumanTaskCommand implements ModelTestCommand<HumanTaskCommand, HumanTaskResponse> {
    private final static Logger logger = LoggerFactory.getLogger(HumanTaskTestCommand.class);

    private transient final TestScript testScript;
    private transient final HumanTaskResponseValidator[] validators;
    private final HumanTaskCommand actualCommand;
    private HumanTaskResponse actualResponse;
    private CommandFailure actualFailure;

    public HumanTaskTestCommand(TestScript testScript, HumanTaskCommand command, HumanTaskResponseValidator[] validators) {
        super(command.getUser(), null, command.actorId);
        this.testScript = testScript;
        this.actualCommand = command;
        this.validators = validators;
    }

    /**
     * Returns the number this CaseTestCommand has inside the {@link TestScript}
     *
     * @return
     */
    @Override
    public int getActionNumber() {
        return testScript.getActionNumber();
    }

    /**
     * Returns the actual command that was (or is to be) executed by the case instance
     * @return
     */
    public HumanTaskCommand getActualCommand() {
        return actualCommand;
    }

    @Override
    public ModelResponse process(HumanTask task) {
        // Just have the actual command do its processing
        return actualCommand.process(task);
    }

    @Override
    public void validate(HumanTask humanTaskActor) throws InvalidCommandException {
        actualCommand.setActor(getActor());
        actualCommand.validate(humanTaskActor);
    }

    /**
     * Returns the response given by the case for the command; can only be accessed after the response has been received.
     * @return
     */
    @Override
    public HumanTaskResponse getActualResponse() {
        return actualResponse;
    }

    /**
     * Returns the failure given back by the case engine for the command.
     * @return
     */
    @Override
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

    @Override
    public String toString() {
        return actualCommand.toString();
    }

    /**
     * Handles the response to this command by validating the result.
     * @param response The response to this CaseTestCommand
     */
    public void handleResponse(HumanTaskResponse response) {
        // Store the actual response.
        this.actualResponse = response;

        // Wait for the CaseModified event to be published
        awaitModelModifiedEvent(response);

        logger.debug("Validating response for test command " + getActionNumber() + ": " + this.getActualCommand());
        // Run the validators. Validators raise an exception for the test script to stop. Typically an assertion error.
        for (int i = 0; i < validators.length; i++) {
            validators[i].validate(this);
        }
    }

    @Override
    public void handleFailure(CommandFailure failure) {
        // Store the actual response.
        this.actualFailure = failure;

        logger.debug("Validating response for test command " + getActionNumber() + ": " + this.getActualCommand());
        // Run the validators. Validators raise an exception for the test script to stop. Typically an assertion error.
        for (int i = 0; i < validators.length; i++) {
            validators[i].validate(this);
        }
    }

    private void awaitModelModifiedEvent(HumanTaskResponse response) {
        // If the response resulted in a CommandFailure, the Case will not persist any events, so
        // we can return immediately.
//        if (response instanceof HumanTaskFailure) {
//            return;
//        }

        // If we ping a case before it has a definition, there will not be a last modified.
        //  But also there will not be any events generated, so we can return immediately.
//        if (response.getLastModified() == null) {
//            return;
//        }

        logger.debug("Awaiting events for test command " + getActionNumber() + ": " + this);

        // Now wait for the event stream listener to have handled all the events.
        getEventListener().awaitTaskModified(response.getLastModified());
    }
}
