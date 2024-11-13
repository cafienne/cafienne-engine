/* 
 * Copyright 2014 - 2019 CaseFabric B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.casefabric.cmmn.test.task;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.casefabric.cmmn.actorapi.command.StartCase;
import com.casefabric.cmmn.definition.CaseDefinition;
import com.casefabric.cmmn.instance.State;
import com.casefabric.cmmn.test.TestScript;
import com.casefabric.json.ValueList;
import com.casefabric.json.ValueMap;
import org.junit.Rule;
import org.junit.Test;

import java.time.Instant;
import java.time.format.DateTimeFormatter;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.casefabric.cmmn.test.TestScript.*;

public class TestPaxAlert {
    private final CaseDefinition definitions = loadCaseDefinition("testdefinition/task/paxalert.xml");

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
        passenger.plus("firstname", "Shady");
        passenger.plus("lastname", "Person");
        ValueList bagageList = pde.withArray("bagage");
        ValueMap bagage = new ValueMap();
        bagage.plus("tagid", "1234");
        bagage.plus("weight", "23");
        bagageList.add(bagage); 
        ValueMap checkin = pde.with("checkin");
        checkin.plus("flight", "KL1234");
        checkin.plus("departuretime", "2015-03-21T12:12:00Z");
        

        StartCase startCase = createCaseCommand(testUser, caseInstanceId, definitions, inputs);
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
