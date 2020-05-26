/* 
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.akka.command;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.akka.actor.command.exception.CommandException;
import org.cafienne.akka.actor.identity.TenantUser;
import org.cafienne.akka.actor.serialization.Manifest;
import org.cafienne.cmmn.akka.command.response.CaseResponse;
import org.cafienne.cmmn.instance.Case;
import org.cafienne.cmmn.instance.PlanItem;
import org.cafienne.cmmn.instance.Transition;
import org.cafienne.cmmn.instance.casefile.ValueMap;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * A command to have a plan item make a certain transition. E.g. complete a task in a case, or suspend a subprocess.
 */
@Manifest
public class MakePlanItemTransition extends CaseCommand {
    private final String planItemName;
    private final String planItemId;
    private final Transition transition;

    private enum Fields {
        transition, planItemName, planItemId
    }

    /**
     * Create a command to transition the plan item with the specified id or name. Note, if only the name is specified, then the command will work on
     * all plan items within the case having the specified name.
     *
     * @param caseInstanceId The id of the case in which to perform this command.
     * @param planItemId     The id of the plan item. In general it is preferred to select a plan item by id, rather than by name. If the plan item id is null or
     *                       left empty, then the value of the name parameter will be considered.
     * @param transition     The transition to make on the plan item(s)
     * @param name           When only the name is specified, then the transition will be made on _all_ plan items within the case having this name, in reverse
     *                       order. If the transition of such a plan item results in a new plan item in the case with the same name, then the command will _not_ be
     *                       invoked on the new plan item
     */
    public MakePlanItemTransition(TenantUser user, String caseInstanceId, String planItemId, Transition transition, String name) {
        super(user, caseInstanceId);
        this.planItemName = name;
        this.planItemId = planItemId;
        this.transition = transition;
    }

    public MakePlanItemTransition(ValueMap json) {
        super(json);
        this.planItemName = readField(json, Fields.planItemName);
        this.planItemId = readField(json, Fields.planItemId);
        this.transition = readEnum(json, Fields.transition, Transition.class);
    }

    public String getPlanItemName() {
        return planItemName;
    }

    public String getPlanItemId() {
        return planItemId;
    }

    public Transition getTransition() {
        return transition;
    }

    @Override
    public String toString() {
        if (planItemId == null) {
            return "Transition " + planItemName + "." + transition;
        } else {
            return "Transition " + planItemId + "." + transition;
        }
    }

    @Override
    public CaseResponse process(Case caseInstance) {
        if (planItemId != null && !planItemId.trim().isEmpty()) {
            PlanItem planItem = caseInstance.getPlanItemById(planItemId);
            if (planItem == null) {
                // Plan item must exist by id
                throw new CommandException("The plan item with id " + planItemId + " could not be found in case " + caseInstance.getId());
            }
            caseInstance.makePlanItemTransition(planItem, transition);
        } else {
            List<PlanItem> planItemsByName = new ArrayList();
            caseInstance.getPlanItems().stream().filter(p -> p.getName().equals(planItemName)).forEach(p -> {
                // System.out.println("Preparing plan item " + p + " with state " + p.getState() + " for transition " + transition);
                planItemsByName.add(p);
            });
            for (int i = planItemsByName.size() - 1; i >= 0; i--) {
                // System.out.println("Now making the transition on plan item " + i + " with current state " + planItemsByName.get(i).getState());
                caseInstance.makePlanItemTransition(planItemsByName.get(i), transition);
            }
        }
        return new CaseResponse(this);
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.write(generator);
        writeField(generator, Fields.planItemName, planItemName);
        writeField(generator, Fields.planItemId, planItemId);
        writeField(generator, Fields.transition, transition);
    }
}
