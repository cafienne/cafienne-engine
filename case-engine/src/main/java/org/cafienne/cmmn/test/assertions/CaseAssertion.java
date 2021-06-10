package org.cafienne.cmmn.test.assertions;

import org.cafienne.cmmn.actorapi.event.plan.PlanItemCreated;
import org.cafienne.cmmn.instance.casefile.Path;
import org.cafienne.cmmn.test.TestScript;
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
    /**
     * Temporary method to quicker convert code
     * @param assertion
     */
    @Deprecated
    public CaseAssertion(CaseAssertion assertion) {
        this(assertion.getTestCommand());
    }


    private static PlanItemCreated getCasePlan(CaseTestCommand testCommand) {
        PublishedEventsAssertion<PlanItemCreated> pea = testCommand.getEventListener().getEvents().filter(PlanItemCreated.class);
        EventFilter<PlanItemCreated> filter = e -> e.getType().equals("CasePlan");
        return pea.filter(filter).getEvents().stream().findFirst().orElse(null);
    }

    public void print() {
        TestScript.debugMessage("Result of step " + testCommand.getActionNumber() +": " + testCommand.caseInstanceString());
    }

    @Override
    public String toString() {
        return testCommand.caseInstanceString();
    }

    /**
     * Returns the set of events that resulted from executing the command
     * @return
     */
    public PublishedEventsAssertion<?> getEvents() {
        return getTestCommand().getEvents();
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
        PublishedEventsAssertion<PlanItemCreated> pea = testCommand.getEventListener().getEvents().filter(caseId).filter(PlanItemCreated.class);
        EventFilter<PlanItemCreated> filter = e -> e.getPlanItemId().equals(identifier) || e.getPlanItemName().equals(identifier);
        return pea.filter(filter).getEvents().stream();
    }

    public CaseFileAssertion assertCaseFile() {
        return caseFileAssertion;
    }

    /**
     * Returns a CaseFileItemAssertion wrapper for the given path
     *
     * @param path item name
     * @return CaseFileItemAssertion
     */
    public CaseFileItemAssertion assertCaseFileItem(Path path) {
        return caseFileAssertion.assertCaseFileItem(path);
    }
}
