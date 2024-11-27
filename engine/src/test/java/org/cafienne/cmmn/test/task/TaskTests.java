/* 
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.test.task;

import org.cafienne.cmmn.test.basic.TestBasic;
import org.cafienne.cmmn.test.basic.TestSimple;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({
    TestBasic.class,
    TestSimple.class,
    TestSubCase.class,
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
