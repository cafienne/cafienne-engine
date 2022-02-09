/* 
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.instance.casefile;

import org.cafienne.actormodel.exception.InvalidCommandException;

/**
 * Thrown if a {@link Path} is not valid according to the CaseDefinition
 */
public class InvalidPathException extends InvalidCommandException {
    public InvalidPathException(String msg) {
        super(msg);
    }
}
