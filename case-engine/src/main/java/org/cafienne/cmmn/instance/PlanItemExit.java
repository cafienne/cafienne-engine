package org.cafienne.cmmn.instance;

import org.cafienne.cmmn.definition.ItemDefinition;
import org.cafienne.cmmn.definition.sentry.ExitCriterionDefinition;
import org.cafienne.cmmn.instance.sentry.CriteriaListener;
import org.cafienne.cmmn.instance.sentry.Criterion;
import org.cafienne.cmmn.instance.sentry.ExitCriterion;
import org.w3c.dom.Element;

public class PlanItemExit extends CriteriaListener<ExitCriterionDefinition, ExitCriterion> {
    PlanItemExit(PlanItem<?> item) {
        super(item, item.getItemDefinition().getExitCriteria());
    }

    @Override
    protected ExitCriterion createCriterion(ExitCriterionDefinition definition) {
        return definition.createInstance(this);
    }

    @Override
    public void satisfy(Criterion<?> criterion) {
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

    @Override
    protected void migrateCriteria(ItemDefinition newItemDefinition) {
        migrateCriteria(newItemDefinition.getExitCriteria());
    }
}
