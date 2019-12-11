/* 
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.test.casefile;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({
    BasicTypes.class,
    CaseFileTransitionTest.class,
    CaseFileTest.class,
    InvalidCaseFileDefinition.class,
    MultiplicityTest.class,
    RepetitiveFileItems.class,
    TestTaskInputMapping.class
    })
public class CaseFileTests {

}
