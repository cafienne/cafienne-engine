/* 
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.test.task;

import org.cafienne.cmmn.test.basic.Basic;
import org.cafienne.cmmn.test.basic.Simple;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({
    Basic.class,
    Simple.class,
    SubCase.class,
    TestPaxAlert.class,
    TestGetListGetDetails.class,
    TestHumanTask.class,
    TestInvalidProcessDefinition.class,
    TestPDFReport.class,
    TestSMTPServer.class,
    TestTaskOutputParameters.class,
    TestTaskOutputValidation.class
    })
public class TaskTests {

}
