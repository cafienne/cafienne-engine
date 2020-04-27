/* 
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.test.task;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.cafienne.akka.actor.identity.TenantUser;
import org.cafienne.cmmn.akka.command.StartCase;
import org.cafienne.cmmn.definition.CaseDefinition;
import org.cafienne.cmmn.instance.State;
import org.cafienne.cmmn.instance.casefile.ValueList;
import org.cafienne.cmmn.instance.casefile.ValueMap;
import org.cafienne.cmmn.test.TestScript;
import org.cafienne.cmmn.test.assertions.CaseAssertion;
import org.junit.Rule;
import org.junit.Test;

import java.time.Instant;
import java.time.format.DateTimeFormatter;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

public class TestPaxAlert {
    private final CaseDefinition definitions = TestScript.getCaseDefinition("testdefinition/task/paxalert.xml");
    private final TenantUser testUser = TestScript.getTestUser("Anonymous");

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(9888);

    @Test
    public void testPaxAlert() {

        startWireMocks();

        String caseInstanceId = "PaxAlertTest";
        TestScript testCase = new TestScript("PaxAlertTest");

        // {"pde":
//        {"passenger": {"firstname": "Shady", "lastname": "Person"},
//            "bagage": {"tagid": "1234", "weight": "23"},
//            "checkin": {"flight": "KL1234", "departuretime": "2015-03-21T12:12:00Z"}}
//          }
        // Now start a case with a child being set within the JSON input
        ValueMap inputs = new ValueMap();
        ValueMap pde = inputs.with("pde");
        ValueMap passenger = pde.with("passenger");
        passenger.putRaw("firstname", "Shady");
        passenger.putRaw("lastname", "Person");
        ValueList bagageList = pde.withArray("bagage");
        ValueMap bagage = new ValueMap();
        bagage.putRaw("tagid", "1234");
        bagage.putRaw("weight", "23");
        bagageList.add(bagage); 
        ValueMap checkin = pde.with("checkin");
        checkin.putRaw("flight", "KL1234");
        checkin.putRaw("departuretime", "2015-03-21T12:12:00Z");
        

        StartCase startCase = new StartCase(testUser, caseInstanceId, definitions, inputs, null);
        testCase.addStep(startCase, caseStarted -> {
            testCase.getEventListener().awaitPlanItemState("Execute background check", State.Completed);
            caseStarted.print();
        });

        testCase.runTest();
    }

    private void startWireMocks() {
        //Start wireMock service for mocking process service calls
        stubFor(post(urlEqualTo("/demo/background"))
                .withHeader("Accept", equalTo("application/json"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"background\": {\"result\": \"HIT\", \"date\":\"" + DateTimeFormatter.ISO_INSTANT.format(Instant.now()) + "\"}}" ))
                );
        stubFor(post(urlEqualTo("/v2/pushes"))
            .withHeader("Accept", equalTo("application/json"))
            .willReturn(aResponse()
                    .withStatus(201)
                        .withHeader("Content-Type", "application/json").withBody("{\"Message\": \"OK\"}")));
    }

}
