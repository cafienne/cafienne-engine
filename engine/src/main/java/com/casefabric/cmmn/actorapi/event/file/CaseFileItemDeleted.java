/*
 * Copyright (C) 2014  Batav B.V.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.casefabric.cmmn.actorapi.event.file;

import com.casefabric.cmmn.instance.State;
import com.casefabric.cmmn.instance.casefile.CaseFileItem;
import com.casefabric.cmmn.instance.casefile.CaseFileItemTransition;
import com.casefabric.infrastructure.serialization.Manifest;
import com.casefabric.json.Value;
import com.casefabric.json.ValueMap;

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
