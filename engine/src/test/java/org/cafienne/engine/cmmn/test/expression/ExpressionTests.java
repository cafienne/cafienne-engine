/* 
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.engine.cmmn.test.expression;

import org.cafienne.engine.cmmn.test.basic.TestPlanning;
import org.cafienne.engine.cmmn.test.basic.TestRepeatRule;
import org.cafienne.engine.cmmn.test.basic.TestRequiredRule;
import org.cafienne.engine.cmmn.test.basic.TestSentry;
import org.cafienne.engine.cmmn.test.task.TestGetListGetDetails;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({
        TestPlanning.class,
        TestRepeatRule.class,
        TestRequiredRule.class,
        TestSentry.class,
        TestGetListGetDetails.class,
        TestTimerExpression.class,
        TestVariousSpelExpressions.class,
        TestVariousSpelExpressions2.class,
        TestCaseFileContextExpressions.class
})
public class ExpressionTests {

}
