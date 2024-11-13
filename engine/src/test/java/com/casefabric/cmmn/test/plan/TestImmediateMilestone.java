package com.casefabric.cmmn.test.plan;

import com.casefabric.cmmn.actorapi.command.StartCase;
import com.casefabric.cmmn.definition.CaseDefinition;
import com.casefabric.cmmn.instance.State;
import com.casefabric.cmmn.instance.Transition;
import com.casefabric.cmmn.test.TestScript;
import com.casefabric.cmmn.test.assertions.CaseAssertion;
import com.casefabric.humantask.actorapi.command.CompleteHumanTask;
import com.casefabric.humantask.actorapi.event.HumanTaskAssigned;
import com.casefabric.humantask.actorapi.event.HumanTaskDueDateFilled;
import com.casefabric.json.ValueMap;
import org.junit.Test;

import static com.casefabric.cmmn.test.TestScript.*;

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
