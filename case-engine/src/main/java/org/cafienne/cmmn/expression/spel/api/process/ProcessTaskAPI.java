package org.cafienne.cmmn.expression.spel.api.process;

import org.cafienne.cmmn.expression.spel.api.APIObject;
import org.cafienne.cmmn.expression.spel.api.cmmn.file.CaseFileAPI;
import org.cafienne.cmmn.expression.spel.api.cmmn.plan.PlanItemAPI;
import org.cafienne.cmmn.expression.spel.api.cmmn.plan.StageAPI;
import org.cafienne.cmmn.expression.spel.api.cmmn.team.CaseTeamAPI;
import org.cafienne.cmmn.instance.Case;
import org.cafienne.cmmn.instance.PlanItem;
import org.cafienne.processtask.instance.ProcessTaskActor;

import java.util.Collection;

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
