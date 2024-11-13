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

package com.casefabric.cmmn.actorapi.command.plan.eventlistener;

import com.casefabric.actormodel.exception.InvalidCommandException;
import com.casefabric.actormodel.identity.CaseUserIdentity;
import com.casefabric.cmmn.actorapi.command.plan.MakePlanItemTransition;
import com.casefabric.cmmn.instance.Case;
import com.casefabric.cmmn.instance.Transition;
import com.casefabric.infrastructure.serialization.Manifest;
import com.casefabric.json.ValueMap;

/**
 * A command to have a plan item make a certain transition. E.g. complete a task in a case, or suspend a subprocess.
 */
@Manifest
public class RaiseEvent extends MakePlanItemTransition {
    /**
     * Create a command to transition the plan item with the specified id or name. Note, if only the name is specified, then the command will work on
     * all plan items within the case having the specified name.
     *
     * @param caseInstanceId The id of the case in which to perform this command.
     * @param identifier     Either planItemId or planItemName. When only the name is specified, then the transition will be made on _all_ plan items within the case having this name, in reverse
     *                       order. If the transition of such a plan item results in a new plan item in the case with the same name, then the command will _not_ be
     *                       invoked on the new plan item.
     */
    public RaiseEvent(CaseUserIdentity user, String caseInstanceId, String identifier) {
        super(user, caseInstanceId, identifier, Transition.Occur);
    }

    public RaiseEvent(ValueMap json) {
        super(json);
    }

    @Override
    public void validate(Case caseInstance) throws InvalidCommandException {
        // Overriding validate to avoid check on case team membership, similar to the logic in CompleteTask.validate
        // However still validating the current state and the state of the surrounding stage
        super.validateTransition(caseInstance);
    }
}
