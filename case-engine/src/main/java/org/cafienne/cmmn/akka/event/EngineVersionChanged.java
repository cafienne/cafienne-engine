package org.cafienne.cmmn.akka.event;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.akka.actor.serialization.Manifest;
import org.cafienne.cmmn.instance.Case;
import org.cafienne.cmmn.instance.CaseInstanceEvent;
import org.cafienne.cmmn.instance.casefile.ValueMap;

import java.io.IOException;

@Manifest
public class EngineVersionChanged extends CaseInstanceEvent {

    private final ValueMap version;

    private enum Fields {
        version
    }

    public EngineVersionChanged(Case caseInstance, ValueMap version) {
        super(caseInstance);
        this.version = version;
    }

    public EngineVersionChanged(ValueMap json) {
        super(json);
        this.version = readMap(json, Fields.version);
    }

    /**
     * Returns the version of the engine that is currently applied in the case
     * @return
     */
    public ValueMap version() {
        return version;
    }

    @Override
    public String toString() {
        return "Case[" + getCaseInstanceId() + "] runs with engine " + version;
    }

    @Override
    public void recover(Case caseInstance) {
        caseInstance.recoverVersion(version);
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.writeCaseInstanceEvent(generator);
        super.writeField(generator, Fields.version, version);
    }
}
