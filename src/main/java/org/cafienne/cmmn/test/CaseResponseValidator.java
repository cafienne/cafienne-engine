/* 
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.test;

@FunctionalInterface
public interface CaseResponseValidator {

    /**
     * The test framework will call this function with the case instance after the command has been handled by the case actor.
     *
     * @param command
     * @throws AssertionError
     */
    void validate(CaseTestCommand command) throws AssertionError;
}
