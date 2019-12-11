package org.cafienne.cmmn.akka.command.response;

import org.cafienne.cmmn.akka.command.AddDiscretionaryItem;
import org.cafienne.cmmn.akka.command.GetDiscretionaryItems;
import org.cafienne.akka.actor.serialization.Manifest;
import org.cafienne.cmmn.instance.casefile.ValueMap;

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
