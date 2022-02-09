/* 
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.test;

import akka.actor.UntypedAbstractActor;

/**
 * A too simple actor that is used by {@link TestScript} to receive messages back from the case system
 *
 */
class ResponseHandlingActor extends UntypedAbstractActor {
    private final TestScript testScript;

    ResponseHandlingActor(TestScript testScript) {
        this.testScript = testScript;
    }

    public void onReceive(Object o) {
        // Just pass back to the test script for further handling.
        testScript.handleResponse(o);
    }
}
