/* 
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.test;

import org.cafienne.cmmn.test.assertions.CaseAssertion;

@FunctionalInterface
public interface CaseValidator {

    /**
     * The test framework will call this function with the case instance after the command has been handled by the case actor.
     *
     * @param assertion
     * @throws AssertionError
     */
    void validate(CaseAssertion assertion) throws AssertionError;
}
