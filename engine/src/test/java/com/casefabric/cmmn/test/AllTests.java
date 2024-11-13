/* 
 * Copyright 2014 - 2019 CaseFabric B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.casefabric.cmmn.test;

import com.casefabric.cmmn.instance.TestValueClasses;
import com.casefabric.cmmn.test.basic.BasicTests;
import com.casefabric.cmmn.test.casefile.CaseFileTests;
import com.casefabric.cmmn.test.expression.ExpressionTests;
import com.casefabric.cmmn.test.plan.CasePlanTests;
import com.casefabric.cmmn.test.sentry.SentryNetworkTests;
import com.casefabric.cmmn.test.task.TaskTests;
import com.casefabric.cmmn.test.team.TeamTests;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({
        BasicTests.class,
        SentryNetworkTests.class,
        CasePlanTests.class,
        CaseFileTests.class,
        ExpressionTests.class,
        TaskTests.class,
        TeamTests.class,
        TestValueClasses.class,
})
public class AllTests {

}
