/* 
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.engine.cmmn.test.basic;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({
    TestBasic.class,
    TestCasePlanExitCriteria.class,
    TestEventListener.class,
    TestHelloWorld.class,
    TestPlanning.class,
    TestRepeatRule.class,
    TestRequiredRule.class,
    TestSentry.class,
    TestSentryRef.class,
    TestSimple.class,
    TestStages.class,
    TestStageCompletion.class,
    TestTimer.class
    })
public class BasicTests {

}
