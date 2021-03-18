package org.cafienne.cmmn.expression.spel.api.cmmn.plan;

import org.cafienne.cmmn.instance.Milestone;

/**
 */
public class MilestoneAPI extends PlanItemAPI<Milestone> {
    MilestoneAPI(Milestone milestone, StageAPI stage) {
        super(milestone, stage);
    }

    @Override
    public String getName() {
        return "milestone";
    }
}
