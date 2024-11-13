/* 
 * Copyright 2014 - 2019 CaseFabric B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.casefabric.cmmn.test.expression;

import com.casefabric.cmmn.test.basic.TestPlanning;
import com.casefabric.cmmn.test.basic.TestRepeatRule;
import com.casefabric.cmmn.test.basic.TestRequiredRule;
import com.casefabric.cmmn.test.basic.TestSentry;
import com.casefabric.cmmn.test.task.TestGetListGetDetails;
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
