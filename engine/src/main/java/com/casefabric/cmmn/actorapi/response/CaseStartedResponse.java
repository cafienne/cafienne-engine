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

package com.casefabric.cmmn.actorapi.response;

import com.casefabric.cmmn.actorapi.command.CaseCommand;
import com.casefabric.infrastructure.serialization.Manifest;
import com.casefabric.json.ValueMap;

/**
 * Response when a StartCase command is sent
 */
@Manifest
public class CaseStartedResponse extends CaseResponseWithValueMap {
    public CaseStartedResponse(CaseCommand command, ValueMap value) {
        super(command, value);
    }

    public CaseStartedResponse(ValueMap json) {
        super(json);
    }
}
