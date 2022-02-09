package org.cafienne.cmmn.expression.spel.api;

import org.cafienne.cmmn.expression.spel.api.cmmn.plan.CaseAPI;
import org.cafienne.cmmn.expression.spel.api.cmmn.plan.PlanItemAPI;
import org.cafienne.cmmn.expression.spel.api.cmmn.team.MemberAPI;
import org.cafienne.cmmn.instance.Case;
import org.cafienne.cmmn.instance.PlanItem;

/**
 * Base class for giving object context to Case expressions
 * Exposes 'case' property.
 */
public abstract class CaseRootObject extends APIRootObject<Case> {
    private final CaseAPI context;

    protected CaseRootObject(Case model) {
        super(model, new MemberAPI(model.getCaseTeam(), model.getCurrentTeamMember()));
        this.context = new CaseAPI(model);
        addContextProperty(this.context, "case", "caseInstance");
    }

    protected Case getCase() {
        return getActor();
    }

    public CaseAPI getCaseInstance() {
        warnDeprecation("getCaseInstance()", "case");
        return context;
    }

    protected void registerPlanItem(PlanItem<?> item) {
        final PlanItemAPI<?> itemContext = this.context.find(item);
        addContextProperty(itemContext, itemContext.getName(), "planItem");
    }
}

