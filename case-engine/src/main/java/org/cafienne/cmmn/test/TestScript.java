/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.test;

import org.cafienne.actormodel.identity.TenantUser;
import org.cafienne.actormodel.response.CommandFailure;
import org.cafienne.actormodel.response.ModelResponse;
import org.cafienne.cmmn.actorapi.command.CaseCommand;
import org.cafienne.cmmn.actorapi.command.team.CaseTeam;
import org.cafienne.cmmn.actorapi.command.team.CaseTeamMember;
import org.cafienne.cmmn.actorapi.command.team.MemberKey;
import org.cafienne.cmmn.definition.CaseDefinition;
import org.cafienne.cmmn.definition.DefinitionsDocument;
import org.cafienne.cmmn.definition.InvalidDefinitionException;
import org.cafienne.cmmn.repository.MissingDefinitionException;
import org.cafienne.cmmn.test.assertions.CaseAssertion;
import org.cafienne.cmmn.test.assertions.FailureAssertion;
import org.cafienne.infrastructure.Cafienne;
import org.cafienne.system.CaseSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.Await;
import scala.concurrent.duration.Duration;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.*;

/**
 * This class enables building test scripts for cases.
 * It internally spans an Actor that communicates a series of commands with the case actor.
 * After a command is processed by the case, the response can be validated through one or more {@link CaseResponseValidator}s.
 * Note, in the validation of the response, new commands can be added to the test script (e.g. to complete a task based
 * on it's plan item id, rather than by name).
 * Note that commands need to hold the identifier of the case. The test script is agnostic of this identifier, and therefore
 * it is possible to add commands that communicate with multiple case instances (typically usefull when a case has one or more
 * sub cases).
 * When the test script completes or aborts, a corresponding method can be invoked. Subclasses of TestScript can implement these to
 * do custom handling of script completion.
 * Because of the asynchronous nature of the underlying Akka framework, the {@link TestScript#runTest()} method may not get
 * feedback from the case actor (due to some error). Therefore this method internally blocks a specified duration and then closes down
 * the actor system.
 * The TestScript uses an in-memory configuration for the Akka system. It assumes that all case instances run within the same JVM, and
 * it therefore also relies internally on passing the Case object itself. This object can be used in the response validation.
 */
public class TestScript {
    private final String testName;
    private final static Logger logger = LoggerFactory.getLogger(TestScript.class);
    private final CaseSystem caseSystem;

    private boolean testCompleted;

    private Deque<ModelTestCommand> commands = new ArrayDeque(); // We need to be able to add elements both at front and end; and execute always the front element
    private ModelTestCommand current; // current test step
    private int actionNumber = 0; // current action number

    /**
     * Listener for CaseInstanceEvent that ought to be published by the Akka system
     */
    private final CaseEventListener eventListener;

    /**
     * Simple helper to retrieve and parse a definitions file containing one or more case definitions
     *
     * @param fileName The name of the file to be read (e.g., testdefinition/basic.case)
     * @return
     */
    public static DefinitionsDocument getDefinitions(String fileName) {
        try {
            return Cafienne.config().repository().DefinitionProvider().read(null, null, fileName);
        } catch (MissingDefinitionException | InvalidDefinitionException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns the first case definition in the definitions file
     *
     * @param fileName
     * @return
     */
    public static CaseDefinition getCaseDefinition(String fileName) throws MissingDefinitionException {
        return getDefinitions(fileName).getFirstCase();
    }

    /**
     * Returns the case definition with the specified identifier from the definitions file
     *
     * @param fileName
     * @return
     */
    public static CaseDefinition getCaseDefinition(String fileName, String caseIdentifier) {
        return getDefinitions(fileName).getCaseDefinition(caseIdentifier);
    }

    /**
     * Helper method to retrieve an invalid definitions document.
     * Throws an assertion if the Definition is missing instead of invalid.
     *
     * @param fileName
     */
    public static void getInvalidDefinition(String fileName) throws InvalidDefinitionException {
        try {
            Cafienne.config().repository().DefinitionProvider().read(null, null, fileName);
        } catch (MissingDefinitionException e) {
            throw new AssertionError(e);
        }

    }

    /**
     * Returns a user context for the specified user name and optional roles
     *
     * @param user
     * @param roles
     * @return
     */
    public static TenantUser getTestUser(final String user, final String... roles) {
        return new TenantUser(user, scala.jdk.CollectionConverters.ListHasAsScala(Arrays.asList(roles)).asScala().toSet(), "hard-coded-test-tenant", false, "", "", true);
    }

    /**
     * Creates a CaseTeam that can be used in StartCase command based upon a list of user contexts
     *
     * @param users The users array can hold case team members, tenant users. Tenant users will be become
     *              members, with the tenant roles that they have passed to them
     * @return
     */
    public static CaseTeam getCaseTeam(Object... users) {
        List<CaseTeamMember> members = new ArrayList<>();
        for (Object user : users) {
            if (user instanceof TenantUser) {
                members.add(getMember((TenantUser) user));
            } else if (user instanceof CaseTeamMember) {
                members.add((CaseTeamMember) user);
            } else {
                throw new IllegalArgumentException("Cannot accept users of type " + user.getClass().getName());
            }
        }
        return CaseTeam.apply(members);
    }

    /**
     * Create a case owner with roles, copies tenant roles, adds additional roles
     *
     * @param user
     * @return
     */
    public static CaseTeamMember getOwner(TenantUser user) {
        return CaseTeamMember.apply(new MemberKey(user.id(), "user"), user.roles(), true);
    }

    /**
     * Create a simple member with roles, copies tenant roles, adds additional roles
     *
     * @param user
     * @return
     */
    public static CaseTeamMember getMember(TenantUser user) {
        return CaseTeamMember.apply(new MemberKey(user.id(), "user"), user.roles(), false);
    }

    /**
     * Create a new {@link TestScript} with the specified name
     *
     * @param testName
     */
    public TestScript(String testName) {
        logger.info("\n\n\t\t============ Creating new test '" + testName + "' ========================\n\n");
        this.testName = testName;
        this.caseSystem = new CaseSystem("Case-Engine-Test-Script");

        // Start listening to the events coming out of the case persistence mechanism
        this.eventListener = new CaseEventListener(this);
        logger.info("Ready to receive responses from the case system for test '" + testName + "'");
    }

    /**
     * Prints a log message to the debug logger
     *
     * @param msg
     */
    public static void debugMessage(Object msg) {
        logger.debug(String.valueOf(msg));
    }

    /**
     * Adds a command to the test script, along with an optional list of validators. The validators will be invoked when the command has been handled by
     * the case, and the test script has received a response back from the case.
     *
     * @param command
     * @param validator
     */
    private void addTestStep(CaseCommand command, CaseResponseValidator validator) {
        commands.addLast(new CaseTestCommand(this, command, validator));
    }

    /**
     * Insert a new test command right after the current test step. Can be used inside validators to
     * add new commands when a response to the command is received.
     *
     * @param command
     * @param validators
     */
    private void insertTestStep(CaseCommand command, CaseResponseValidator validators) {
        commands.addFirst(new CaseTestCommand(this, command, validators));
    }

    /**
     * Add a command that is expected to fail, and then invoke the validator with the failure to
     * do more assertions.
     *
     * @param command
     * @param validator
     */
    public void assertStepFails(CaseCommand command, FailureValidator validator) {
        addTestStep(command, e -> validator.validate(new FailureAssertion(e)));
    }

    /**
     * Check that command fails, without any further validations.
     *
     * @param command
     */
    public void assertStepFails(CaseCommand command) {
        addTestStep(command, e -> new FailureAssertion(e));
    }

    /**
     * Check that command fails, without any further validations.
     *
     * @param command
     */
    public void insertStepFails(CaseCommand command, FailureValidator validator) {
        insertTestStep(command, e -> validator.validate(new FailureAssertion(e)));
    }

    /**
     * Add a command, and use the validator to check the result.
     * Command is expected to succeed (should not return with CommandFailure)
     *
     * @param command
     * @param validator
     */
    public void addStep(CaseCommand command, CaseValidator validator) {
        addTestStep(command, e -> validator.validate(new CaseAssertion(e)));
    }

    /**
     * Add a command that should return without failure.
     *
     * @param command
     */
    public void addStep(CaseCommand command) {
        addTestStep(command, e -> new CaseAssertion(e));
    }

    public void insertStep(CaseCommand command, CaseValidator validator) {
        insertTestStep(command, e -> validator.validate(new CaseAssertion(e)));
    }

    public int getActionNumber() {
        return actionNumber;
    }

    /**
     * Executes the next test command.
     */
    private void continueTest() {
        if (commands.isEmpty()) {
            finish(null);
            return;
        }

        // Set the current command and action number.
        current = commands.removeFirst();
        actionNumber++;

        // A bit of a hack right here. PingCommand is a special command that can be used to have the
        // test system wait for some time, in order to have the case instance give some room
        // for handling internal timer events.
        if (current.getActualCommand() instanceof PingCommand) {
            PingCommand waitCommand = (PingCommand) current.getActualCommand();
            waitCommand.awaitCompletion();
        }

        logger.debug("Sending test command " + current.getActionNumber() + ": [" + current + "] to case " + current.getActorId());

        eventListener.sendCommand(current);

    }

    /**
     * Override this method to have a callback upon completion of the test script
     */
    public void complete() {
    }

    /**
     * Override this method to have a callback upon error of the test script
     *
     * @param t
     */
    public void abort(Throwable t) {
    }

    /**
     * Starts the test with a maximum duration of 10 minutes.
     */
    public void runTest() {
        // By default 10 minutes available for your debug session :)
        runTest(20 * 1000);
    }

    /**
     * Starts the test script. The test will wait at max the specified duration before it will close down the actor system.
     */
    public void runTest(long maximumDuration) {
        // Now start the first command and wait until the test is completed
        continueTest();
        awaitCompletion(maximumDuration);

        if (testCompleted) {
            logger.info("\n\n\t\t============ Completed test '" + testName + "' ========================\n\n");
        } else {
            logger.info("\n\n\t\t============ Could not complete test '" + testName + "' ========================\n\n");
        }
    }

    private void awaitCompletion(long maximumDuration) {
        synchronized (this) {
            while (!testCompleted && maximumDuration > 0) {
                try {
                    final long SECOND = 1000;
                    wait(SECOND);
                    maximumDuration -= SECOND;
                } catch (InterruptedException e) {

                }
                if (!testCompleted && maximumDuration < 10000) // Only print if 10 seconds left
                    logger.warn("Waiting another " + (maximumDuration / 1000) + " seconds for completion of test '" + testName + "'");
            }
        }

        closeDown();

        if (!testCompleted) {
            throw new AssertionError("Test '" + testName + "' was not completed; got stuck at action " + actionNumber + " (command " + current + ")");
        }

        if (exceptionFromTest != null) {
            try {
                FileOutputStream fos = new FileOutputStream(getNextErrorFile());
                exceptionFromTest.printStackTrace(new PrintStream(fos));
                fos.close();
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }

            // Try to throw the exception as "unwrapped" as possible, without adding a throws clause
            if (exceptionFromTest instanceof RuntimeException) {
                throw (RuntimeException) exceptionFromTest;
            } else if (exceptionFromTest instanceof Error) {
                throw (Error) exceptionFromTest;
            } else { // Well ... then let's just wrap it
                throw new RuntimeException(exceptionFromTest);
            }
        }
    }

    private File getNextErrorFile() {
        int index = 0;
        File logDirectory = new File("logs");
        if (!logDirectory.exists()) {
            logDirectory.mkdirs();
        }
        String fileName = testName + "_" + index + "_error.txt";
        File nextErrorFile = new File(logDirectory, fileName);
        while (nextErrorFile.exists()) {
            index++;
            fileName = testName + "_" + index + "_error.txt";
            nextErrorFile = new File(logDirectory, fileName);
            if (index > 1000) {
                System.err.println("Clean up your error files");
                System.exit(-1);
            }
        }
        return nextErrorFile;
    }

    private Throwable exceptionFromTest;

    private synchronized void finish(Throwable exception) {
        // First invoke callbacks, if any
        if (exception != null) {
            abort(exception);
        } else {
            complete();
        }

        testCompleted = true;
        this.exceptionFromTest = exception;
        notifyAll();
    }

    private void closeDown() {
        logger.debug("Closing down actor system");
        try {
            Await.result(caseSystem.system().terminate(), Duration.create(10, "seconds"));
        } catch (Exception ex) {
            logger.error("ISSUE terminating the actor system " + ex.getMessage());
        }
    }

    /**
     * Returns the event listener for this TestScript.
     *
     * @return
     */
    public CaseEventListener getEventListener() {
        return eventListener;
    }

    /**
     * Method used by {@link ResponseHandlingActor} to notify incoming messages from the case system (i.e., response to
     * the commands sent by the test script to the case instances).
     *
     * @param response
     */
    void handleResponse(Object response) {
        if (!(response instanceof ModelResponse)) {
            // quite strange, let's just quit right here right now.
            finish(new AssertionError("Received an unexpected message while executing test script; message:\n:" + response));
            return;
        }

        if (current == null) {
            // quite strange; apparently we never sent a command and still got a response, let's just quit right here right now.
            finish(new AssertionError("Received an unexpected message while executing test script; message:\n:" + response));
            return;
        }

        // Run the validations, if available, and then continue the test script with the next step.
        try {
            if (response instanceof CommandFailure) {
                current.handleFailure((CommandFailure) response);
            } else {
                current.handleResponse((ModelResponse) response);
            }
        } catch (Throwable t) {
            // One of the validators raised an exception, so finish the script here and now with that exception
            finish(t);
            return;
        }
        continueTest();
    }

    public CaseSystem getCaseSystem() {
        return caseSystem;
    }
}
