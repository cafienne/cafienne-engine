package org.cafienne.cmmn.test.assertions;

import org.cafienne.cmmn.akka.event.PlanItemCreated;
import org.cafienne.cmmn.test.assertions.file.CaseFileAssertion;
import org.cafienne.cmmn.test.assertions.file.CaseFileItemAssertion;
import org.cafienne.cmmn.test.filter.EventFilter;
import org.cafienne.cmmn.test.CaseTestCommand;

import java.util.stream.Stream;

/**
 * Some assertions for the case.
 * Note: the case assertion is itself a plan item assertion for the case plan for convenience.
 * Additionally, on StageAssertion assertions on plan items based on identifier only search in Stage level plan items,
 * whereas similar CaseAssertion assertions search in all plan items of the case.
 */
public class CaseAssertion extends StageAssertion {
    private final CaseFileAssertion caseFileAssertion;

    /**
     * Creates a new CaseAssertion around the case instance inside the test command
     *
     * @param testCommand
     */
    public CaseAssertion(CaseTestCommand testCommand) {
        super(testCommand, getCasePlan(testCommand));
        this.caseFileAssertion = new CaseFileAssertion(testCommand);
    }

    private static PlanItemCreated getCasePlan(CaseTestCommand testCommand) {
        PublishedEventsAssertion<PlanItemCreated> pea = new PublishedEventsAssertion(testCommand.getEventListener().getEvents()).filter(PlanItemCreated.class);
        EventFilter<PlanItemCreated> filter = e -> e.getType().equals("CasePlan");
        return pea.filter(filter).getEvents().stream().findFirst().orElse(null);
    }

    @Override
    public String toString() {
        return testCommand.caseInstanceString();
    }

    /**
     * Note - this method overrides {@link StageAssertion#getPlanItems(String)}, causing all
     * assert methods like assertPlanItem, assertPlanItems, assertTask, etc, when executed on a CaseAssertion to
     * search in all plan items of the case, whereas a specific StageAssertion.assertPlanItem will only
     * look for plan items in the stage it relates to, and not in child stages.
     *
     * @param identifier
     * @return
     */
    @Override
    protected Stream<PlanItemCreated> getPlanItems(String identifier) {
        PublishedEventsAssertion<PlanItemCreated> pea = new PublishedEventsAssertion(testCommand.getEventListener().getEvents()).filter(caseId).filter(PlanItemCreated.class);
        EventFilter<PlanItemCreated> filter = e -> e.getPlanItemId().equals(identifier) || e.getPlanItemName().equals(identifier);
        return pea.filter(filter).getEvents().stream();
    }

    /**
     * Returns a CaseFileItemAssertion wrapper for the given path
     *
     * @param fileItemPath item name
     * @return CaseFileItemAssertion
     */
    public CaseFileItemAssertion assertCaseFileItem(String fileItemPath) {
        return caseFileAssertion.assertCaseFileItem(fileItemPath);
    }
}
