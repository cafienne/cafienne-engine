package org.cafienne.cmmn.actorapi.event;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.cmmn.instance.Case;
import org.cafienne.infrastructure.serialization.Fields;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.ValueMap;
import org.cafienne.processtask.actorapi.event.ProcessEnded;

import java.io.IOException;

@Manifest
public class CaseOutputFilled extends CaseBaseEvent {
    public final ValueMap output;
    public CaseOutputFilled(Case actor, ValueMap outputParameters) {
        super(actor);
        this.output = outputParameters;
    }

    public CaseOutputFilled(ValueMap json) {
        super(json);
        this.output = json.readMap(Fields.output);
    }

    @Override
    public void updateState(Case actor) {
        // No need to update state, as that is also updated through the case file events
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.writeCaseEvent(generator);
        writeField(generator, Fields.output, output);
    }
}
