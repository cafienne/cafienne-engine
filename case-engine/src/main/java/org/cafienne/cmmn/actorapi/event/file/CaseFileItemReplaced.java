/* 
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.actorapi.event.file;

import org.cafienne.akka.actor.serialization.Manifest;
import org.cafienne.akka.actor.serialization.json.Value;
import org.cafienne.akka.actor.serialization.json.ValueMap;
import org.cafienne.cmmn.instance.State;
import org.cafienne.cmmn.instance.casefile.CaseFileItem;
import org.cafienne.cmmn.instance.casefile.CaseFileItemTransition;

/**
 * Event caused by creation of a CaseFileItem
 */
@Manifest
public class CaseFileItemReplaced extends CaseFileEvent {
    public CaseFileItemReplaced(CaseFileItem item, Value<?> newValue) {
        super(item, State.Available, CaseFileItemTransition.Replace, newValue);
    }

    public CaseFileItemReplaced(ValueMap json) {
        super(json);
    }
}
