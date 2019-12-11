package org.cafienne.cmmn.akka.event.debug;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.akka.actor.serialization.Manifest;
import org.cafienne.cmmn.instance.Case;
import org.cafienne.cmmn.instance.casefile.ValueMap;
import org.cafienne.cmmn.instance.sentry.Sentry;

import java.io.IOException;

@Manifest
public class SentryEvent extends DebugEvent {
    private Sentry sentry;

    public SentryEvent(Case caseInstance) {
        super(caseInstance);
    }

    public SentryEvent(ValueMap json) {
        super(json);
    }

    public void addMessage(String msg, Sentry sentry) {
        this.sentry = sentry;
        this.addMessage(msg);
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.write(generator);
        if (sentry != null) {
            generator.writeFieldName(this.sentry.getCriterion().getDefinition().getType());
            sentry.toJson().print(generator);
        }
    }
}
