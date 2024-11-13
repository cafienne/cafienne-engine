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

import com.casefabric.actormodel.identity.CaseUserIdentity;
import com.casefabric.cmmn.actorapi.command.CaseCommand;
import com.casefabric.cmmn.actorapi.response.GetDiscretionaryItemsResponse;
import com.casefabric.cmmn.instance.Case;
import com.casefabric.cmmn.instance.DiscretionaryItem;
import com.casefabric.infrastructure.serialization.Fields;
import com.casefabric.infrastructure.serialization.Manifest;
import com.casefabric.json.ValueList;
import com.casefabric.json.ValueMap;

/**
 */
@Manifest
public class GetDiscretionaryItems extends CaseCommand {
    /**
     * This will retrieve a list of valid discretionary items
     * 
     * @param caseInstanceId
     *          The id of the case instance to get the discretionary items for
     */
    public GetDiscretionaryItems(CaseUserIdentity user, String caseInstanceId) {
        super(user, caseInstanceId);
    }

    public GetDiscretionaryItems(ValueMap json) {
        super(json);
    }

    @Override
    public void processCaseCommand(Case caseInstance) {
        // Convert the response to JSON
        ValueMap node = new ValueMap();
        node.plus(Fields.caseInstanceId, getCaseInstanceId());
        node.plus(Fields.name, caseInstance.getDefinition().getName());
        ValueList items = node.withArray(Fields.discretionaryItems);

        caseInstance.getDiscretionaryItems().forEach((DiscretionaryItem item) -> {
            items.add(item.asJson());
        });

        setResponse(new GetDiscretionaryItemsResponse(this, node));
    }
}
