package org.cafienne.cmmn.expression.spel.api.process;

import org.cafienne.cmmn.expression.spel.api.APIObject;
import org.cafienne.processtask.instance.ProcessTaskActor;

public class ProcessTaskAPI extends APIObject<ProcessTaskActor> {
    public ProcessTaskAPI(ProcessTaskActor actor) {
        super(actor);
        addPropertyReader("id", actor::getId);
        addPropertyReader("name", actor::getName);
        addPropertyReader("tenant", actor::getTenant);
        addPropertyReader("parent", actor::getParentActorId);
        addPropertyReader("root", actor::getRootActorId);
    }
}
