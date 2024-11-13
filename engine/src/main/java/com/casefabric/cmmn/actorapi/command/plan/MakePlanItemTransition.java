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

package com.casefabric.cmmn.actorapi.command.plan;

import com.fasterxml.jackson.core.JsonGenerator;
import com.casefabric.actormodel.exception.CommandException;
import com.casefabric.actormodel.exception.InvalidCommandException;
import com.casefabric.actormodel.identity.CaseUserIdentity;
import com.casefabric.cmmn.actorapi.command.CaseCommand;
import com.casefabric.cmmn.actorapi.response.CaseNotModifiedResponse;
import com.casefabric.cmmn.actorapi.response.CaseResponse;
import com.casefabric.cmmn.instance.Case;
import com.casefabric.cmmn.instance.PlanItem;
import com.casefabric.cmmn.instance.Transition;
import com.casefabric.infrastructure.serialization.Fields;
import com.casefabric.infrastructure.serialization.Manifest;
import com.casefabric.json.ValueMap;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * A command to have a plan item make a certain transition. E.g. complete a task in a case, or suspend a subprocess.
 */
@Manifest
public class MakePlanItemTransition extends CaseCommand {
    private final String identifier;
    private final Transition transition;

    /**
     * Create a command to transition the plan item with the specified id or name. Note, if only the name is specified, then the command will work on
     * all plan items within the case having the specified name.
     *
     * @param caseInstanceId The id of the case in which to perform this command.
     * @param identifier     Either planItemId or planItemName. When only the name is specified, then the transition will be made on _all_ plan items within the case having this name, in reverse
     *                       order. If the transition of such a plan item results in a new plan item in the case with the same name, then the command will _not_ be
     *                       invoked on the new plan item.
     * @param transition     The transition to make on the plan item(s)
     */
    public MakePlanItemTransition(CaseUserIdentity user, String caseInstanceId, String identifier, Transition transition) {
        super(user, caseInstanceId);
        this.identifier = identifier;
        this.transition = transition;
    }

    public MakePlanItemTransition(ValueMap json) {
        super(json);
        this.identifier = json.readString(Fields.identifier);
        this.transition = json.readEnum(Fields.transition, Transition.class);
    }

    public String getIdentifier() {
        return identifier;
    }

    public Transition getTransition() {
        return transition;
    }

    @Override
    public String toString() {
        return "Transition " + identifier + "." + transition;
    }

    @Override
    public void validate(Case caseInstance) throws InvalidCommandException {
        super.validate(caseInstance);
        validateTransition(caseInstance);
    }

    protected void validateTransition(Case caseInstance) {
        getTargetPlanItems(caseInstance).forEach(item -> item.validateTransition(transition));
    }

    private List<PlanItem<?>> getTargetPlanItems(Case caseInstance) {
        List<PlanItem<?>> targets = new ArrayList<PlanItem<?>>();
        if (identifier != null && !identifier.trim().isEmpty()) {
            // First check if we can find the plan item by id.
            //  If none is found, let's see if the identifier matches any plan item name.
            PlanItem<?> planItem = caseInstance.getPlanItemById(identifier);
            if (planItem != null) {
                // When Plan item exists by id, return just that
                targets.add(planItem);
            } else {
                // Trying to find plan items with a matching name; but only those in Active stages.
                caseInstance.getPlanItems()
                        .stream()
                        .filter(item -> item.getName().equals(identifier) && item.stageAllowsActivity())
                        .forEach(targets::add);
            }
        }

        if (targets.isEmpty()) {
            throw new CommandException("There is no plan item with identifier '" + identifier + "' in case " + caseInstance.getId());
        }
        return targets;
    }

    @Override
    public void processCaseCommand(Case caseInstance) {
        boolean transitioned = false;
        List<PlanItem<?>> planItemsByName = getTargetPlanItems(caseInstance);
        for (int i = planItemsByName.size() - 1; i >= 0; i--) {
            transitioned = transitioned || caseInstance.makePlanItemTransition(planItemsByName.get(i), transition);
        }

        CaseResponse response = transitioned ? new CaseResponse(this) : new CaseNotModifiedResponse(this);
        setResponse(response);
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.write(generator);
        writeField(generator, Fields.identifier, identifier);
        writeField(generator, Fields.transition, transition);
    }
}
