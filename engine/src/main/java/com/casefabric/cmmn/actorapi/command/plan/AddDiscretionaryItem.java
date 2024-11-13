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
import com.casefabric.cmmn.actorapi.response.AddDiscretionaryItemResponse;
import com.casefabric.cmmn.definition.DiscretionaryItemDefinition;
import com.casefabric.cmmn.instance.Case;
import com.casefabric.cmmn.instance.DiscretionaryItem;
import com.casefabric.cmmn.instance.PlanItem;
import com.casefabric.infrastructure.serialization.Fields;
import com.casefabric.infrastructure.serialization.Manifest;
import com.casefabric.json.ValueMap;
import com.casefabric.util.Guid;

import java.io.IOException;

/**
 * Adds a discretionary item to the case. This will only have effect if the discretionary item can actually be planned currently in the case.
 */
@Manifest
public class AddDiscretionaryItem extends CaseCommand {
    private final String name;
    private final String planItemId;
    private final String parentId;
    private final String definitionId;
    private transient DiscretionaryItem discretionaryItem;
    private transient PlanItem<?> parentItem;

    /**
     * Create a command to add a new plan item to the case, based on a discretionary item definition with the specified name.
     *
     * @param caseInstanceId The id of the case in which to perform this command.
     * @param name           The name of the plan item to be added. This name must refer to a discretionary item in one of the planning tables of the case.
     * @param newPlanItemId  Optional plan item id with which the new plan item will be created. If the id is null or empty, a new {@link Guid} will be created.
     * @param definitionId The id of the discretionary item; can be used instead of the name.
     * @param parentId The id of the plan item that contains this discretionary item (i.e., the stage or human task in which it is being planned).
     */
    public AddDiscretionaryItem(CaseUserIdentity user, String caseInstanceId, String name, String definitionId, String parentId, String newPlanItemId) {
        super(user, caseInstanceId);
        this.name = name;
        this.planItemId = (newPlanItemId == null || newPlanItemId.isEmpty()) ? new Guid().toString() : newPlanItemId;
        this.definitionId = definitionId;
        this.parentId = parentId;
    }

    public AddDiscretionaryItem(ValueMap json) {
        super(json);
        this.name = json.readString(Fields.name);
        this.planItemId = json.readString(Fields.planItemId);
        this.definitionId = json.readString(Fields.definitionId);
        this.parentId = json.readString(Fields.parentId);
    }

    @Override
    public void validate(Case caseInstance) {
        super.validate(caseInstance);
        PlanItem<?> alreadyExisting = caseInstance.getPlanItemById(planItemId);
        if (alreadyExisting != null) {
            throw new InvalidCommandException("Cannot plan a discretionary item named '" + name + "' with the specified id " + planItemId + ", because the case already has a plan item with that id");
        }
        parentItem = caseInstance.getPlanItemById(parentId);
        if (parentItem == null) {
            throw new InvalidCommandException("Cannot plan a discretionary item named '" + name + "' because the parent item with id '"+parentId+"' cannot be found in the case");
        }
        DiscretionaryItemDefinition definition = caseInstance.getDefinition().getElement(definitionId);
        discretionaryItem = definition.createInstance(parentItem);
        if (!discretionaryItem.isPlannable()) {
            throw new InvalidCommandException("Cannot plan a discretionary item named '" + name + "'. It may not be applicable at this moment");
        }

        if (!discretionaryItem.isAuthorized()) {
            throw new InvalidCommandException("No authorization available to plan a discretionary item named '" + name + "'");
        }
    }

    @Override
    public void processCaseCommand(Case caseInstance) {
        discretionaryItem.plan(planItemId);
        setResponse(new AddDiscretionaryItemResponse(this, new ValueMap("planItemId", planItemId)));
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.writeModelCommand(generator);
        writeField(generator, Fields.name, name);
        writeField(generator, Fields.planItemId, planItemId);
        writeField(generator, Fields.definitionId, definitionId);
        writeField(generator, Fields.parentId, parentId);
    }
}
