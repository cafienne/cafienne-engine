package org.cafienne.cmmn.instance;

import org.cafienne.cmmn.instance.sentry.Criterion;
import org.cafienne.cmmn.instance.sentry.EntryCriterion;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.Collection;

class PlanItemEntry {
    private final Collection<EntryCriterion> entryCriteria = new ArrayList<>();
    private final PlanItem planItem;

    PlanItemEntry(PlanItem planItem) {
        this.planItem = planItem;
    }

    void connect() {
        planItem.getItemDefinition().getEntryCriteria().forEach(c -> entryCriteria.add(planItem.getStage().getEntryCriterion(c, planItem)));
    }

    /**
     * Method invoked by the various state machines when the plan item becomes available;
     * typically determines whether it must be started or should wait for entry criteria to become active
     *
     * @param transition
     */
    public void beginLifeCycle(Transition transition) {
        if (entryCriteria.isEmpty()) { // No entry criteria means get started immediately
            planItem.addDebugInfo(() -> planItem + ": no EntryCriteria found, making transition " + transition);
            planItem.makeTransition(transition);
        } else {
            // Evaluate sentries to see whether one is already active, and, if so, make the transition
            for (EntryCriterion criterion : entryCriteria) {
                if (criterion.isSatisfied()) {
                    planItem.addDebugInfo(() -> planItem + ": an EntryCriterion is satisfied, making transition " + transition);
                    satisfy(criterion);
                    return;
                }
            }
            planItem.addDebugInfo(() -> planItem + ": Not making transition because no entry criteria are satisfied");
        }
    }

    public boolean isEmpty() {
        return entryCriteria.isEmpty();
    }

    public void dumpMemoryStateToXML(Element planItemXML) {
        if (entryCriteria.isEmpty()) {
            // Only create a comment tag if we actually have entry criteria
            return;
        }
        planItemXML.appendChild(planItemXML.getOwnerDocument().createComment(" Entry criteria "));
        for (Criterion criterion : entryCriteria) {
            criterion.dumpMemoryStateToXML(planItemXML, true);
        }
    }

    public void satisfy(EntryCriterion criterion) {
        if (planItem.getIndex() == 0 && (planItem.getState() == State.Null || planItem.getState() == State.Available)) {
            // In this scenario, the entry criterion is triggered on the very first instance of the plan item,
            //  and also for the very first time. Therefore we should not yet repeat, but only make the
            //  entry transition.
            planItem.addDebugInfo(() -> criterion + " is satisfied and will trigger "+planItem.getEntryTransition());
            planItem.makeTransition(planItem.getEntryTransition());
        } else {
            // In all other cases we have to check whether or not to create a repeat item, and, if so,
            //  initiate that with the entry transition
            planItem.addDebugInfo(() -> criterion + " is satisfied and will repeat " + planItem);
            planItem.repeat();
        }
    }
}
