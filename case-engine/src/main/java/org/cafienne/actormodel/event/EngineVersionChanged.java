package org.cafienne.actormodel.event;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.actormodel.ModelActor;
import org.cafienne.infrastructure.CafienneVersion;
import org.cafienne.infrastructure.serialization.Fields;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.ValueMap;

import java.io.IOException;

@Manifest
public class EngineVersionChanged extends BaseModelEvent<ModelActor<?,?>> {

    private final CafienneVersion version;

    public EngineVersionChanged(ModelActor<?,?> actor, CafienneVersion version) {
        super(actor);
        this.version = version;
    }

    public EngineVersionChanged(ValueMap json) {
        super(json);
        this.version = new CafienneVersion(readMap(json, Fields.version));
    }

    @Override
    public void updateState(ModelActor<?,?> actor) {
        actor.setEngineVersion(this.version);
    }

    @Override
    public String getDescription() {
        return super.getDescription() +" to " + version.description();
    }

    /**
     * Returns the version of the engine that is currently applied in the case
     * @return
     */
    public CafienneVersion version() {
        return version;
    }

    @Override
    public String toString() {
        return "Engine version changed to " + version;
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.writeModelEvent(generator);
        super.writeField(generator, Fields.version, version.json());
    }
}
