/*
 * Copyright 2014 - 2019 CaseFabric B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.casefabric.cmmn.test.sentry;

import com.casefabric.cmmn.test.casefile.TestRepetitiveFileItems;
import com.casefabric.cmmn.test.expression.TestVariousSpelExpressions;
import com.casefabric.cmmn.test.task.TestGetListGetDetails;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({
        TestMilestone.class,
        TestRepetitiveFileItems.class,
        TestGetListGetDetails.class,
        TestVariousSpelExpressions.class
    })
public class SentryNetworkTests {

}
