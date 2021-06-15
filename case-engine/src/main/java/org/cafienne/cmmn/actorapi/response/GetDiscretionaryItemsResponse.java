package org.cafienne.cmmn.actorapi.response;

import org.cafienne.cmmn.actorapi.command.plan.GetDiscretionaryItems;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.ValueMap;

/**
 * Response to a {@link GetDiscretionaryItems} command
 */
@Manifest
public class GetDiscretionaryItemsResponse extends CaseResponseWithValueMap {
    public GetDiscretionaryItemsResponse(GetDiscretionaryItems command, ValueMap value) {
        super(command, value);
    }

    public GetDiscretionaryItemsResponse(ValueMap json) {
        super(json);
    }
}
