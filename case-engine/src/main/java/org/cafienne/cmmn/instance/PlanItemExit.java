package org.cafienne.cmmn.instance;

import org.cafienne.cmmn.instance.sentry.Criterion;
import org.cafienne.cmmn.instance.sentry.EntryCriterion;
import org.cafienne.cmmn.instance.sentry.ExitCriterion;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.Collection;

public class PlanItemExit {
    private final Collection<ExitCriterion> exitCriteria = new ArrayList<>();
    private final PlanItem planItem;

    PlanItemExit(PlanItem planItem) {
        this.planItem = planItem;
    }

    public void connect() {
        planItem.getItemDefinition().getExitCriteria().forEach(c -> exitCriteria.add(new ExitCriterion(planItem, c)));
    }

    void release() {
        exitCriteria.forEach(c -> c.release());
    }

    public void satisfy(ExitCriterion criterion) {
        planItem.addDebugInfo(() -> criterion + " is satisfied, triggering exit on " + planItem);
        release();
        planItem.makeTransition(planItem.getExitTransition());
    }

    void dumpMemoryStateToXML(Element planItemXML) {
        if (exitCriteria.isEmpty()) {
            // Only create a comment tag if we actually have entry criteria
            return;
        }
        planItemXML.appendChild(planItemXML.getOwnerDocument().createComment(" Exit criteria "));
        for (Criterion criterion : exitCriteria) {
            criterion.dumpMemoryStateToXML(planItemXML, true);
        }
    }
}
