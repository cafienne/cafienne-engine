package org.cafienne.cmmn.instance;

import org.cafienne.cmmn.instance.sentry.Criterion;
import org.cafienne.cmmn.instance.sentry.EntryCriterion;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.Collection;

public class PlanItemEntry {
    private final Collection<EntryCriterion> entryCriteria = new ArrayList<>();
    private final PlanItem planItem;

    PlanItemEntry(PlanItem planItem) {
        this.planItem = planItem;
    }

    public void connect() {
        planItem.getItemDefinition().getEntryCriteria().forEach(c -> entryCriteria.add(new EntryCriterion(planItem, c)));
    }

    void release() {
        entryCriteria.forEach(c -> c.release());
    }

    /**
     * Method invoked by the various state machines when the plan item becomes available;
     * typically determines whether it must be started or should wait for entry criteria to become active
     *
     * @param transition
     */
    public void beginLifeCycle(Transition transition) {
        if (entryCriteria.isEmpty()) { // No entry criteria means get started immediately
            planItem.addDebugInfo(() -> planItem + ": Starting lifecycle with " + transition + " because there are no entry criteria defined");
            planItem.makeTransition(transition);
        } else {
            if (earlyBird != null) {
                planItem.addDebugInfo(() -> planItem + ": Starting lifecycle with " + transition + " because of " + earlyBird);
                handleCriterionSatiesfied(earlyBird);
            } else {
                // Evaluate sentries to see whether one is already active, and, if so, make the transition
                for (EntryCriterion criterion : entryCriteria) {
                    if (criterion.isSatisfied()) {
                        planItem.addDebugInfo(() -> planItem + ": an EntryCriterion is satisfied, making transition " + transition);
                        handleCriterionSatiesfied(criterion);
                        return;
                    }
                }
                planItem.addDebugInfo(() -> planItem + ": Not starting lifecycle with " + transition + " because none of the entry criteria is satisfied");
            }
        }
    }

    public boolean isEmpty() {
        return entryCriteria.isEmpty();
    }

    private EntryCriterion earlyBird = null;

    public void satisfy(EntryCriterion criterion) {
        if (planItem.getState() == State.Null) {
            // Criterion is an early bird considering our state, let's put it in the waiting room until our lifecycle starts
            earlyBird = criterion;
            return;
        }
        handleCriterionSatiesfied(criterion);
    }

    private void handleCriterionSatiesfied(EntryCriterion criterion) {
        if (planItem.getIndex() == 0 && planItem.getState() == State.Available) {
            // In this scenario, the entry criterion is triggered on the very first instance of the plan item,
            //  and also for the very first time. Therefore we should not yet repeat, but only make the
            //  entry transition.
            planItem.addDebugInfo(() -> criterion + " is satisfied and will trigger "+planItem.getEntryTransition());
            if (this.willNotRepeat()) {
                release();
            }
            planItem.makeTransition(planItem.getEntryTransition());
        } else {
            // In all other cases we have to check whether or not to create a repeat item, and, if so,
            //  initiate that with the entry transition
            planItem.addDebugInfo(() -> criterion + " is satisfied and will repeat " + planItem);
            release();
            planItem.repeat();
        }
    }

    private boolean willNotRepeat() {
        return planItem.getItemDefinition().getPlanItemControl().getRepetitionRule().isDefault();
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
}
