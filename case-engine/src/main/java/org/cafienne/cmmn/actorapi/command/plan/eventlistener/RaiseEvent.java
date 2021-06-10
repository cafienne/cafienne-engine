/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.actorapi.command.plan.eventlistener;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.akka.actor.command.exception.CommandException;
import org.cafienne.akka.actor.command.exception.InvalidCommandException;
import org.cafienne.akka.actor.identity.TenantUser;
import org.cafienne.akka.actor.serialization.Fields;
import org.cafienne.akka.actor.serialization.Manifest;
import org.cafienne.akka.actor.serialization.json.ValueMap;
import org.cafienne.cmmn.actorapi.command.CaseCommand;
import org.cafienne.cmmn.actorapi.response.CaseResponse;
import org.cafienne.cmmn.instance.Case;
import org.cafienne.cmmn.instance.PlanItem;
import org.cafienne.cmmn.instance.Transition;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * A command to have a plan item make a certain transition. E.g. complete a task in a case, or suspend a subprocess.
 */
@Manifest
public class RaiseEvent extends CaseCommand {
    private final String identifier;

    /**
     * Create a command to transition the plan item with the specified id or name. Note, if only the name is specified, then the command will work on
     * all plan items within the case having the specified name.
     *
     * @param caseInstanceId The id of the case in which to perform this command.
     * @param identifier     Either planItemId or planItemName. When only the name is specified, then the transition will be made on _all_ plan items within the case having this name, in reverse
     *                       order. If the transition of such a plan item results in a new plan item in the case with the same name, then the command will _not_ be
     *                       invoked on the new plan item.
     */
    public RaiseEvent(TenantUser user, String caseInstanceId, String identifier) {
        super(user, caseInstanceId);
        this.identifier = identifier;
    }

    public RaiseEvent(ValueMap json) {
        super(json);
        this.identifier = readField(json, Fields.identifier);
    }

    public String getIdentifier() {
        return identifier;
    }

    @Override
    public String toString() {
        return identifier + ".Occur";
    }

    @Override
    public void validate(Case caseInstance) throws InvalidCommandException {
        // Disable case team membership, similar to the logic in CompleteTask.validate
    }

    @Override
    public CaseResponse process(Case caseInstance) {
        if (identifier != null && !identifier.trim().isEmpty()) {
            PlanItem planItem = caseInstance.getPlanItemById(identifier);
            if (planItem != null) {
                // When Plan item exists by id
                caseInstance.makePlanItemTransition(planItem, Transition.Occur);
                return new CaseResponse(this);
            }
        }
        //when the Plan Item is not found by id, check if it is found by name.
        //if the name was not set, it will use the planItemId as name.
        List<PlanItem> planItemsByName = new ArrayList<PlanItem>();
        caseInstance.getPlanItems().stream().filter(p -> p.getName().equals(identifier)).forEach(p -> planItemsByName.add(p));
        if (planItemsByName.isEmpty()) {
            throw new CommandException("There is no plan item with identifier " + identifier + " in case " + caseInstance.getId());
        }
        for (int i = planItemsByName.size() - 1; i >= 0; i--) {
            caseInstance.makePlanItemTransition(planItemsByName.get(i), Transition.Occur);
        }
        return new CaseResponse(this);
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.write(generator);
        writeField(generator, Fields.identifier, identifier);
    }
}
