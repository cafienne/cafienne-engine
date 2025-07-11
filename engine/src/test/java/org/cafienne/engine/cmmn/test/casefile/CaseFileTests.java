/* 
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.engine.cmmn.test.casefile;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({
    TestBasicTypes.class,
    TestCaseFileTransition.class,
    TestCaseFile.class,
    TestPath.class,
    TestInvalidCaseFileDefinition.class,
    TestMultiplicity.class,
    TestNewCaseFile.class,
    TestRepetitiveFileItems.class,
    TestTaskInputMapping.class,
    TestPathValueResolver.class
    })
public class CaseFileTests {

}
