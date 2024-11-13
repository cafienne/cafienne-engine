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
import com.casefabric.actormodel.exception.InvalidCommandException;
import com.casefabric.actormodel.identity.CaseUserIdentity;
import com.casefabric.cmmn.actorapi.command.CaseCommand;
import com.casefabric.cmmn.instance.Case;
import com.casefabric.cmmn.instance.PlanItem;
import com.casefabric.cmmn.instance.Transition;
import com.casefabric.infrastructure.serialization.Fields;
import com.casefabric.infrastructure.serialization.Manifest;
import com.casefabric.json.ValueMap;

import java.io.IOException;

@Manifest
public class MakeCaseTransition extends CaseCommand {
    private final Transition transition;

    /**
     * Triggers the specified transition on the case (effectively on the case plan).
     *
     * @param caseInstanceId The id of the case in which to perform this command.
     * @param transition     The transition to be executed on the case
     */
    public MakeCaseTransition(CaseUserIdentity user, String caseInstanceId, Transition transition) {
        super(user, caseInstanceId);
        this.transition = transition;
    }

    public MakeCaseTransition(ValueMap json) {
        super(json);
        this.transition = json.readEnum(Fields.transition, Transition.class);
    }

    public Transition getTransition() {
        return transition;
    }

    @Override
    public String toString() {
        return "Transition Case." + transition;
    }

    @Override
    public void validate(Case caseInstance) throws InvalidCommandException {
        super.validate(caseInstance);
        caseInstance.getCasePlan().validateTransition(transition);
    }

    @Override
    public void processCaseCommand(Case caseInstance) {
        PlanItem<?> casePlan = caseInstance.getCasePlan();
        caseInstance.makePlanItemTransition(casePlan, transition);
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.write(generator);
        writeField(generator, Fields.transition, transition);
    }
}
