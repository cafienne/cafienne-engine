package org.cafienne.cmmn.instance;

import org.cafienne.cmmn.instance.sentry.Criterion;
import org.cafienne.cmmn.instance.sentry.CriteriaListener;
import org.cafienne.cmmn.instance.sentry.EntryCriterion;
import org.w3c.dom.Element;

public class PlanItemEntry extends CriteriaListener<EntryCriterion> {

    PlanItemEntry(PlanItem target) {
        super(target);
    }

    /**
     * Connect the plan item to the sentry network
     */
    public void connect() {
        item.getItemDefinition().getEntryCriteria().forEach(c -> criteria.add(new EntryCriterion(this, c)));
        if (! criteria.isEmpty()) {
            item.addDebugInfo(() -> "Connected " + item + " to " + criteria.size() +" entry criteria");
        }
    }

    /**
     * Disconnect the plan item from the sentry network
     */
    void release() {
        item.addDebugInfo(() -> "Releasing all " + criteria.size() + " entry criteria for " + item);
        criteria.forEach(c -> c.release());
    }

    /**
     * Method invoked by the various state machines when the plan item becomes available;
     * typically determines whether it must be started or should wait for entry criteria to become active
     *
     * @param transition
     */
    public void beginLifeCycle(Transition transition) {
        if (criteria.isEmpty()) { // No entry criteria means get started immediately
            item.addDebugInfo(() -> item + ": Starting lifecycle with " + transition + " because there are no entry criteria defined");
            item.makeTransition(transition);
        } else {
            if (earlyBird != null) {
                item.addDebugInfo(() -> item + ": Starting lifecycle with " + transition + " because of " + earlyBird);
                handleCriterionSatiesfied(earlyBird);
            } else {
                // Evaluate sentries to see whether one is already active, and, if so, make the transition
                for (EntryCriterion criterion : criteria) {
                    if (criterion.isSatisfied()) {
                        item.addDebugInfo(() -> item + ": an EntryCriterion is satisfied, making transition " + transition);
                        handleCriterionSatiesfied(criterion);
                        return;
                    }
                }
                item.addDebugInfo(() -> item + ": Not starting lifecycle with " + transition + " because none of the entry criteria is satisfied");
            }
        }
    }

    public boolean isEmpty() {
        return criteria.isEmpty();
    }

    private EntryCriterion earlyBird = null;

    public void satisfy(EntryCriterion criterion) {
        if (item.getState() == State.Null) {
            // Criterion is an early bird considering our state, let's put it in the waiting room until our lifecycle starts
            earlyBird = criterion;
            return;
        }
        handleCriterionSatiesfied(criterion);
    }

    private void handleCriterionSatiesfied(EntryCriterion criterion) {
        if (item.getIndex() == 0 && item.getState() == State.Available) {
            // In this scenario, the entry criterion is triggered on the very first instance of the plan item,
            //  and also for the very first time. Therefore we should not yet repeat, but only make the
            //  entry transition.
            item.addDebugInfo(() -> criterion + " is satisfied and will trigger "+ item.getEntryTransition());
            if (this.willNotRepeat()) {
                release();
            }
            item.makeTransition(item.getEntryTransition());
        } else {
            // In all other cases we have to check whether or not to create a repeat item, and, if so,
            //  initiate that with the entry transition
            item.addDebugInfo(() -> criterion + " is satisfied and will repeat " + item);
            release();
            item.repeat();
        }
    }

    private boolean willNotRepeat() {
        return item.getItemDefinition().getPlanItemControl().getRepetitionRule().isDefault();
    }

    public void dumpMemoryStateToXML(Element planItemXML) {
        if (criteria.isEmpty()) {
            // Only create a comment tag if we actually have entry criteria
            return;
        }
        planItemXML.appendChild(planItemXML.getOwnerDocument().createComment(" Entry criteria "));
        for (Criterion criterion : criteria) {
            criterion.dumpMemoryStateToXML(planItemXML, true);
        }
    }
}
