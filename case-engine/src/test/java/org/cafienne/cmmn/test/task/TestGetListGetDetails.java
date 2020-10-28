/* 
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.test.task;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.cafienne.cmmn.akka.command.MakePlanItemTransition;
import org.cafienne.cmmn.akka.command.StartCase;
import org.cafienne.cmmn.akka.command.casefile.item.CreateCaseFileItem;
import org.cafienne.cmmn.akka.event.plan.PlanItemTransitioned;
import org.cafienne.cmmn.definition.CaseDefinition;
import org.cafienne.cmmn.instance.State;
import org.cafienne.cmmn.instance.Transition;
import org.cafienne.akka.actor.serialization.json.ValueMap;
import org.cafienne.cmmn.test.TestScript;
import org.cafienne.cmmn.test.filter.EventFilter;
import org.cafienne.akka.actor.identity.TenantUser;
import org.junit.Rule;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

/**
 * The flow tested in this case, is to get a list of identifiers from a web service,
 * an subsequently get the details for each of the identifiers.
 * The list is returned by the web service as a JSON array, and this is mapped into
 * a repeating CaseFileItem. Upon creation of each such a CaseFileItem, the details are fetched.
 * However, no more than 3 times are the details fetched (although this is not yet checked in the test case)
 */
public class TestGetListGetDetails {

    private static final int PORT_NUMBER = 18087; // TODO: have wiremock pick the port number

    @Rule
    public WireMockRule wireMockRule;

    // The identifiers that the backend contains; used to compose the mock response
    private static final String[] KEY_LIST = {"A", "B", "C", "D", "E", "F", "G", "H", "I", "J"};

    @Test
    public void testGetListGetDetails() {
        String caseInstanceId = "GetListGetDetailsTest";
        TestScript testCase = new TestScript(caseInstanceId);
        CaseDefinition xml = TestScript.getCaseDefinition("testdefinition/task/getlist-getdetail.xml");

        TenantUser user = TestScript.getTestUser("anonymous");

        /**
         *  Start the case
         */
        StartCase startCase = new StartCase(user, caseInstanceId, xml, null, null);

        /**
         *  When the case starts, GetCasesList & GetFirstCase tasks will be in available state
         */
        testCase.addStep(startCase, casePlan -> {
            casePlan.print();
            casePlan.assertPlanItem("GetList").assertLastTransition(Transition.Create, State.Available, State.Null);
            casePlan.assertPlanItem("GetDetails").assertLastTransition(Transition.Create, State.Available, State.Null);
        });

        FailingPlanItemFilter filterForGetListFailures = new FailingPlanItemFilter();

        // Create the CaseFileItem Request only with HTTPPort
        // This will only activate GetList which will eventually fail as the MockService is not configured
        // So GetList goes to Failed state & GetDetails remains in Available state
        ValueMap requestObject = new ValueMap();
        requestObject.putRaw("port", PORT_NUMBER);
        CreateCaseFileItem createChild = new CreateCaseFileItem(user, caseInstanceId, requestObject.cloneValueNode(), "HTTP-Configuration");
        testCase.addStep(createChild, casePlan -> {
            casePlan.print();

            // Now wait for the first failure event
            testCase.getEventListener().awaitPlanItemTransitioned("GetList", filterForGetListFailures);
        });

        // Reactivate the GetCasesList task; but since the backend is not yet up, it should again lead to failure of GetList
        testCase.addStep(new MakePlanItemTransition(user, caseInstanceId, "GetList", Transition.Reactivate), casePlan -> {
            casePlan.print();

            // Wait for the second failure event
            testCase.getEventListener().awaitPlanItemTransitioned("GetList", filterForGetListFailures);

            casePlan.assertPlanItem("GetList").assertLastTransition(Transition.Fault, State.Failed, State.Active);
            casePlan.assertPlanItem("GetDetails").assertLastTransition(Transition.Create, State.Available, State.Null);

            // Now add the stubs, so that we can re-activate the GetList task.
            startWiremock();
        });

        // Now reactive GetList, since stubs have started.
        // This should also lead to 3 times GetDetails task being activated and completed.
        MakePlanItemTransition reactivateCase = new MakePlanItemTransition(user, caseInstanceId, "GetList", Transition.Reactivate);
        testCase.addStep(reactivateCase, casePlan -> {
            casePlan.print();

            // First wait until the GetList task has become completed
            testCase.getEventListener().awaitPlanItemState("GetList", State.Completed);

            // Now wait until we have the 3 GetDetails tasks getting completed; we keep track of them in a Set
            final Set<PlanItemTransitioned> completedGetDetailsTasks = new HashSet();
            testCase.getEventListener().awaitPlanItemTransitioned("GetDetails", event -> {
                if (event.getCurrentState().equals(State.Completed)) {
                    // Found a new one! Register it in the Set ...
                    completedGetDetailsTasks.add(event);
                }
                // We're successful upon 3rd event
                return completedGetDetailsTasks.size() == 3;
            });

            wireMockRule.stop();
        });

        testCase.runTest();
    }

    /**
     * This is a somewhat clumsy helper class.
     * Since the CaseEventListener monitors and matches repeated filters on all events,
     * a simple filter on GetList to fail would not be sufficient, as in this test the GetList call will fail twice.
     * Hence we work around it with this filter.
     */
    class FailingPlanItemFilter implements EventFilter<PlanItemTransitioned> {
        private PlanItemTransitioned firstFailure;

        @Override
        public boolean matches(PlanItemTransitioned event) {
            if (!event.getCurrentState().equals(State.Failed)) {
                return false;
            }
            if (firstFailure == null) {
                // This is the first failure; preserve a reference to it, so we can detect it when the filter is used for the second test step that fails.
                firstFailure = event;
                return true;
            } else if (event == firstFailure) {
                // This is the second time the filter is used, but we encounter the first failure event, so not the one we're waiting for
                return false;
            } else {
                // This is the second failure
                return true;
            }
        }
    }

    private void startWiremock() {
         wireMockRule = new WireMockRule(PORT_NUMBER);
        //Start wireMock service for mocking process service calls
        wireMockRule.stubFor(get(urlEqualTo("/getListWebService"))
                .withHeader("Accept", equalTo("application/json"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(getJsonList())));
        // For each key, also create a 'details' stub
        for (String key : KEY_LIST) {
            getStubForDetails(key);
        }
        wireMockRule.start();
    }




    private void getStubForDetails(String detailsKey) {
        String detailsBody = ("{ '_2' : { 'description': 'details of " + detailsKey + "', 'id':'" + detailsKey + "'}}");
        wireMockRule.stubFor(get(urlMatching("/details/" + detailsKey))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(detailsBody)));
    }

    private String getJsonList() {
        StringBuffer jsonList = new StringBuffer("{'_2' :[");
        boolean first = true;
        for (String key : KEY_LIST) {
            if (!first) {
                jsonList.append(",");
            }
            first = false;
            jsonList.append("{'id' : '" + key + "'}");
        }
        jsonList.append("]}");
        return jsonList.toString();
    }
}
