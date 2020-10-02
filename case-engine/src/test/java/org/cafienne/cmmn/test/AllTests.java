/* 
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.test;

import org.cafienne.cmmn.instance.ValueTests;
import org.cafienne.cmmn.instance.process.StringTemplateTest;
import org.cafienne.cmmn.test.basic.BasicTests;
import org.cafienne.cmmn.test.casefile.CaseFileTests;
import org.cafienne.cmmn.test.expression.ExpressionTests;
import org.cafienne.cmmn.test.task.TaskTests;
import org.cafienne.cmmn.test.team.TeamTests;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({
        BasicTests.class,
        CaseFileTests.class,
        ExpressionTests.class,
        TaskTests.class,
        TeamTests.class,
        ValueTests.class,
        StringTemplateTest.class
})
public class AllTests {

}
