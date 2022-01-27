/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.actorapi;

import org.cafienne.actormodel.message.UserMessage;
import org.cafienne.cmmn.instance.Case;

public interface CaseMessage extends UserMessage {
    @Override
    default Class<Case> actorClass() {
        return Case.class;
    }
}
