package org.cafienne.cmmn.instance;

import org.cafienne.cmmn.instance.sentry.Criterion;
import org.cafienne.cmmn.instance.sentry.CriteriaListener;
import org.cafienne.cmmn.instance.sentry.ExitCriterion;
import org.w3c.dom.Element;

public class PlanItemExit extends CriteriaListener<ExitCriterion> {

    PlanItemExit(PlanItem<?> target) {
        super(target);
    }

    public void connect() {
        item.getItemDefinition().getExitCriteria().forEach(c -> criteria.add(new ExitCriterion(this, c)));
    }

    void release() {
        criteria.forEach(c -> c.release());
    }

    public void satisfy(ExitCriterion criterion) {
        item.addDebugInfo(() -> criterion + " is satisfied, triggering exit on " + item);
        release();
        item.makeTransition(item.getExitTransition());
    }

    void dumpMemoryStateToXML(Element planItemXML) {
        if (criteria.isEmpty()) {
            // Only create a comment tag if we actually have entry criteria
            return;
        }
        planItemXML.appendChild(planItemXML.getOwnerDocument().createComment(" Exit criteria "));
        for (Criterion<?> criterion : criteria) {
            criterion.dumpMemoryStateToXML(planItemXML, true);
        }
    }
}
