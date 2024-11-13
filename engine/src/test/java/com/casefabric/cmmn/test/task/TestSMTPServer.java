/* 
 * Copyright 2014 - 2019 CaseFabric B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.casefabric.cmmn.test.task;

import com.casefabric.cmmn.actorapi.command.StartCase;
import com.casefabric.cmmn.definition.CaseDefinition;
import com.casefabric.cmmn.instance.State;
import com.casefabric.cmmn.test.TestScript;
import com.casefabric.json.ValueMap;
import org.junit.Test;

import java.util.Base64;

import static com.casefabric.cmmn.test.TestScript.*;

/**
 *
 *
 */
public class TestSMTPServer {
    private final CaseDefinition definitions = loadCaseDefinition("testdefinition/task/smtpcall.xml");


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

        StartCase startCase = createCaseCommand(testUser, caseInstanceId, definitions, inputs);
        testCase.addStep(startCase, casePlan -> {
            casePlan.print();

            // Wait until sending the email has failed (as there is no mail server running, the task must fail...)
            testCase.getEventListener().awaitPlanItemState("Send test email", State.Failed);
        });

        testCase.runTest();
    }


}
