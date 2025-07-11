package org.cafienne.engine.cmmn.test.plan;

import org.cafienne.engine.cmmn.actorapi.command.StartCase;
import org.cafienne.engine.cmmn.definition.CaseDefinition;
import org.cafienne.engine.cmmn.instance.State;
import org.cafienne.engine.cmmn.instance.Transition;
import org.cafienne.engine.cmmn.test.TestScript;
import org.cafienne.engine.cmmn.test.assertions.CaseAssertion;
import org.cafienne.engine.humantask.actorapi.command.CompleteHumanTask;
import org.cafienne.engine.humantask.actorapi.event.HumanTaskAssigned;
import org.cafienne.engine.humantask.actorapi.event.HumanTaskDueDateFilled;
import org.cafienne.json.ValueMap;
import org.junit.Test;

import static org.cafienne.engine.cmmn.test.TestScript.*;

public class TestImmediateMilestone {
    private final CaseDefinition definitions = loadCaseDefinition("testdefinition/milestonedependency.xml");

    @Test
    public void testImmediateMilestone() {
        String caseInstanceId = "MilestoneDependencyTest";
        TestScript testCase = new TestScript("MilestoneDependencyTest");
        ValueMap greeting = new ValueMap();

        StartCase startCase = createCaseCommand(testUser, caseInstanceId, definitions, greeting);
        testCase.addStep(startCase, CaseAssertion::print);

        testCase.runTest();
    }
}
