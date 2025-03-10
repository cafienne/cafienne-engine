package org.cafienne.actormodel.event;

import org.cafienne.actormodel.ModelActor;
import org.cafienne.json.ValueMap;

public abstract class CaseSystemEvent extends BaseModelEvent<ModelActor> {
    protected CaseSystemEvent(ModelActor actor) {
        super(actor);
    }

    protected CaseSystemEvent(ValueMap json) {
        super(json);
    }
}
