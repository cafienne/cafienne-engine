/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.akka.command;

import org.cafienne.akka.actor.command.ModelCommand;
import org.cafienne.akka.actor.command.exception.CommandException;
import org.cafienne.akka.actor.command.exception.InvalidCommandException;
import org.cafienne.akka.actor.command.response.ModelResponse;
import org.cafienne.cmmn.akka.command.response.CaseResponse;
import org.cafienne.akka.actor.command.exception.MissingTenantException;
import org.cafienne.cmmn.instance.Case;
import org.cafienne.cmmn.akka.event.CaseEvent;
import org.cafienne.akka.actor.serialization.json.ValueMap;
import org.cafienne.akka.actor.identity.TenantUser;

/**
 * A {@link Case} instance is designed to handle various AkkaCaseCommands, such as {@link StartCase}, {@link MakePlanItemTransition}, etc.
 * Each CaseCommand must implement it's own logic within the case, through the optional {@link ModelCommand#validate} and the mandatory {@link CaseCommand#process} methods.
 * When the case has succesfully handled the command, it will persist the resulting {@link CaseEvent}s, and send a reply back, see {@link CaseResponse}.
 */
public abstract class CaseCommand extends ModelCommand<Case> {
    /**
     * Create a new command that can be sent to the case.
     *
     * @param user           The user that issues this command.
     * @param caseInstanceId The id of the case in which to perform this command.
     * @throws MissingTenantException If the user context does not have a tenant field.
     */
    protected CaseCommand(TenantUser user, String caseInstanceId) {
        super(user, caseInstanceId);
    }

    protected CaseCommand(ValueMap json) {
        super(json);
    }

    @Override
    public final Class<Case> actorClass() {
        return Case.class;
    }

    /**
     * The id of the case on which to perform the command
     *
     * @return
     */
    public String getCaseInstanceId() {
        return actorId;
    }

    /**
     * Before the case starts processing the command, it will first ask to validate the command.
     * The default implementation is to check whether the case definition is available (i.e., whether StartCase command has been triggered before this command).
     * Implementations can override this method to implement their own validation logic.
     * Implementations may throw the {@link InvalidCommandException} if they encounter a validation error
     *
     * @param caseInstance
     * @throws InvalidCommandException If the command is invalid
     */
    public void validate(Case caseInstance) throws InvalidCommandException {
        // Validate case team membership
        validateCaseTeamMembership(caseInstance);
    }

    /**
     * This method validates the case team membership of the tenant user that sent this command
     * @param caseInstance
     */
    protected void validateCaseTeamMembership(Case caseInstance) {
        caseInstance.getCaseTeam().validateMembership(getUser());
    }

    /**
     * Method invoked by the case in order to perform the actual command logic on the case.
     *
     * @param caseInstance
     * @return
     * @throws CommandException Implementations of this method may throw this exception if a failure happens while processing the command
     */
    public abstract ModelResponse process(Case caseInstance);
}
