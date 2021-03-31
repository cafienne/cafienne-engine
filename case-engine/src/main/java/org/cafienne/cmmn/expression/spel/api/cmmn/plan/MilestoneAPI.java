package org.cafienne.cmmn.expression.spel.api.cmmn.plan;

import org.cafienne.cmmn.instance.Milestone;

/**
 */
public class MilestoneAPI extends PlanItemAPI<Milestone> {
    MilestoneAPI(CaseAPI caseAPI, Milestone milestone, StageAPI stage) {
        super(caseAPI, milestone, stage);
    }

    @Override
    public String getName() {
        return "milestone";
    }
}
