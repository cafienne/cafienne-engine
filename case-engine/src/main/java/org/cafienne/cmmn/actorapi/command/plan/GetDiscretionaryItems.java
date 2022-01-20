/* 
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.actorapi.command.plan;

import org.cafienne.actormodel.identity.CaseUserIdentity;
import org.cafienne.cmmn.actorapi.command.CaseCommand;
import org.cafienne.cmmn.actorapi.response.CaseResponse;
import org.cafienne.cmmn.actorapi.response.GetDiscretionaryItemsResponse;
import org.cafienne.cmmn.definition.DiscretionaryItemDefinition;
import org.cafienne.cmmn.instance.Case;
import org.cafienne.cmmn.instance.DiscretionaryItem;
import org.cafienne.infrastructure.serialization.Fields;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.ValueList;
import org.cafienne.json.ValueMap;

import java.util.Collection;

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
    public CaseResponse process(Case caseInstance) {
        // Convert the response to JSON
        ValueMap node = new ValueMap();
        node.plus(Fields.caseInstanceId, getCaseInstanceId());
        node.plus(Fields.name, caseInstance.getDefinition().getName());
        ValueList items = node.withArray(Fields.discretionaryItems);

        caseInstance.getDiscretionaryItems().forEach((DiscretionaryItem item) -> {
            items.add(item.asJson());
        });

        return new GetDiscretionaryItemsResponse(this, node);
    }
}
