/* 
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.actorapi.event.file;

import org.cafienne.cmmn.instance.State;
import org.cafienne.cmmn.instance.casefile.CaseFileItem;
import org.cafienne.cmmn.instance.casefile.CaseFileItemTransition;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.Value;
import org.cafienne.json.ValueMap;

/**
 * Event caused by creation of a CaseFileItem
 */
@Manifest
public class CaseFileItemDeleted extends CaseFileItemTransitioned {
    public CaseFileItemDeleted(CaseFileItem item) {
        super(item, State.Discarded, CaseFileItemTransition.Delete, Value.NULL);
    }

    public CaseFileItemDeleted(ValueMap json) {
        super(json);
    }
}