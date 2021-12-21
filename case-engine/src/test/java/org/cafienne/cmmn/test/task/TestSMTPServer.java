/* 
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.test.task;

import org.cafienne.cmmn.actorapi.command.StartCase;
import org.cafienne.cmmn.definition.CaseDefinition;
import org.cafienne.cmmn.instance.State;
import org.cafienne.cmmn.test.TestScript;
import org.cafienne.cmmn.test.TestUser;
import org.cafienne.json.ValueMap;
import org.junit.Test;

import java.util.Base64;

/**
 *
 *
 */
public class TestSMTPServer {
    private final CaseDefinition definitions = TestScript.getCaseDefinition("testdefinition/task/smtpcall.xml");
    private final TestUser testUser = TestScript.getTestUser("Anonymous");


    @Test
    public void testSmtpProcess() {

        String caseInstanceId = "SMTP";
        TestScript testCase = new TestScript("SMTP");

        // Now start a case with a child being set within the JSON input
        ValueMap inputs = new ValueMap();
        ValueMap request = inputs.with("Request");
        request.plus("from", "Joop");
        request.plus("to", "Piet");
        request.plus("subject", "The engine is cool");
        request.plus("body", "Thank you for your contributions. You're still in office?");
        request.plus("replyTo", "Jan");

        String attachmentContent = Base64.getEncoder().encodeToString("Hello, how are you?".getBytes());
        request.plus("attachment", attachmentContent);
        request.plus("filename", "abc.txt");

        StartCase startCase = testCase.createCaseCommand(testUser, caseInstanceId, definitions, inputs);
        testCase.addStep(startCase, casePlan -> {
            casePlan.print();

            // Wait until sending the email has failed (as there is no mail server running, the task must fail...)
            testCase.getEventListener().awaitPlanItemState("Send test email", State.Failed);
        });

        testCase.runTest();
    }


}
