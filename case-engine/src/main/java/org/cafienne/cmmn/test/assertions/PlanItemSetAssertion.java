package org.cafienne.cmmn.test.assertions;

import java.util.ArrayList;
import java.util.List;

import org.cafienne.cmmn.instance.State;

/**
 * Some assertions for collections of plan items with the same name in a stage (typically used when a plan item is repeatable)
 */
public class PlanItemSetAssertion {
    private final List<PlanItemAssertion> planItems = new ArrayList<>();
    private final String identifier;

    PlanItemSetAssertion(String identifier) {
        this.identifier = identifier;
    }

    void add(PlanItemAssertion pia) {
        planItems.add(pia);
    }

    /**
     * Returns the plan item assertions within the set
     *
     * @return
     */
    public List<PlanItemAssertion> getPlanItems() {
        return planItems;
    }

    public PlanItemAssertion first() {
        return planItems.get(0);
    }

    /**
     * Returns a new PlanItemSetAssertion with only those plan items that have the expected state.
     *
     * @param expectedState
     * @return
     */
    public PlanItemSetAssertion filter(State expectedState) {
        PlanItemSetAssertion pias = new PlanItemSetAssertion(identifier);
        for (PlanItemAssertion planItemAssertion : planItems) {
            if (planItemAssertion.getState().equals(expectedState)) {
                pias.add(planItemAssertion);
            }
        }
        return pias;
    }

    /**
     * Asserts that the plan items within the set have at least these states. I.e., for each state there must be at least
     * one plan item in the collection having this state.
     *
     * @param expectedStates
     */
    public PlanItemSetAssertion assertStates(State... expectedStates) {
        for (State state : expectedStates) {
            assertAtLeastOneWithState(state);
        }
        return this;
    }

    /**
     * Asserts that the last plan item in this set is not repeating.
     *
     * @return
     */
    public PlanItemSetAssertion assertNoMoreRepetition() {
        planItems.get(planItems.size() - 1).assertNoRepetition();
        return this;
    }

    /**
     * Asserts that the last plan item in this set is repeating.
     *
     * @return
     */
    public PlanItemSetAssertion assertRepeats() {
        planItems.get(planItems.size() - 1).assertRepeats();
        return this;
    }

    /**
     * Assert that at least one plan item has the expected state
     *
     * @param expectedState
     */
    public PlanItemSetAssertion assertAtLeastOneWithState(State expectedState) {
        for (PlanItemAssertion planItemAssertion : planItems) {
            if (planItemAssertion.getState().equals(expectedState)) {
                return this;
            }
        }
        throw new AssertionError("There is no plan item with state " + expectedState + " in the collection of plan items named " + identifier);
    }

    /**
     * Assert that there are plan items within the set
     */
    public PlanItemSetAssertion assertNotEmpty() {
        if (planItems.isEmpty()) {
            throw new AssertionError("There is no plan item named '" + identifier + "'");
        }
        return this;
    }

    /**
     * Asserts that there are a certain number of plan items within the set.
     *
     * @param expectedNumberOfPlanItems
     */
    public PlanItemSetAssertion assertSize(int expectedNumberOfPlanItems) {
        if (planItems.size() != expectedNumberOfPlanItems) {
            throw new AssertionError("Expecting " + expectedNumberOfPlanItems + " plan items '" + identifier + "', but found " + planItems.size());
        }
        return this;
    }
}
