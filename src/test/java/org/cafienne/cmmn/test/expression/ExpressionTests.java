/* 
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.test.expression;

import org.cafienne.cmmn.test.basic.Planning;
import org.cafienne.cmmn.test.basic.RepeatRule;
import org.cafienne.cmmn.test.basic.RequiredRule;
import org.cafienne.cmmn.test.basic.SentryTest;
import org.cafienne.cmmn.test.task.TestGetListGetDetails;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({
        Planning.class,
        RepeatRule.class,
        RequiredRule.class,
        SentryTest.class,
        TestGetListGetDetails.class,
        TestTimerExpression.class,
        VariousSpelExpressions.class,
        VariousSpelExpressions2.class,
        CaseFileContextExpressions.class
})
public class ExpressionTests {

}
