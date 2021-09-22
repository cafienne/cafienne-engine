/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.actorapi.command.plan;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.actormodel.exception.CommandException;
import org.cafienne.actormodel.exception.InvalidCommandException;
import org.cafienne.actormodel.identity.TenantUser;
import org.cafienne.cmmn.actorapi.command.CaseCommand;
import org.cafienne.cmmn.actorapi.response.CaseResponse;
import org.cafienne.cmmn.instance.Case;
import org.cafienne.cmmn.instance.PlanItem;
import org.cafienne.cmmn.instance.Stage;
import org.cafienne.cmmn.instance.Transition;
import org.cafienne.infrastructure.serialization.Fields;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.ValueMap;

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
    public MakePlanItemTransition(TenantUser user, String caseInstanceId, String identifier, Transition transition) {
        super(user, caseInstanceId);
        this.identifier = identifier;
        this.transition = transition;
    }

    public MakePlanItemTransition(ValueMap json) {
        super(json);
        this.identifier = readField(json, Fields.identifier);
        this.transition = readEnum(json, Fields.transition, Transition.class);
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
                        .filter(item -> item.getName().equals(identifier) && item.hasActiveParent())
                        .forEach(targets::add);
            }
        }

        if (targets.isEmpty()) {
            throw new CommandException("There is no plan item with identifier '" + identifier + "' in case " + caseInstance.getId());
        }
        return targets;
    }

    @Override
    public CaseResponse process(Case caseInstance) {
        List<PlanItem<?>> planItemsByName = getTargetPlanItems(caseInstance);
        for (int i = planItemsByName.size() - 1; i >= 0; i--) {
            caseInstance.makePlanItemTransition(planItemsByName.get(i), transition);
        }
        return new CaseResponse(this);
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.write(generator);
        writeField(generator, Fields.identifier, identifier);
        writeField(generator, Fields.transition, transition);
    }
}
