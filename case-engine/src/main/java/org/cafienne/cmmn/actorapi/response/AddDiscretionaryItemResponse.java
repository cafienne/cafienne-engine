package org.cafienne.cmmn.actorapi.response;

import org.cafienne.cmmn.actorapi.command.plan.AddDiscretionaryItem;
import org.cafienne.cmmn.actorapi.command.plan.GetDiscretionaryItems;
import org.cafienne.actormodel.serialization.Manifest;
import org.cafienne.json.ValueMap;

/**
 * Response to a {@link GetDiscretionaryItems} command
 */
@Manifest
public class AddDiscretionaryItemResponse extends CaseResponseWithValueMap {
    public AddDiscretionaryItemResponse(AddDiscretionaryItem command, ValueMap value) {
        super(command, value);
    }

    public AddDiscretionaryItemResponse(ValueMap json) {
        super(json);
    }
}
