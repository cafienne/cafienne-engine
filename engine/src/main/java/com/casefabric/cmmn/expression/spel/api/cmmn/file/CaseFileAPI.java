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

package com.casefabric.cmmn.expression.spel.api.cmmn.file;

import com.casefabric.cmmn.expression.spel.api.APIObject;
import com.casefabric.cmmn.instance.Case;
import com.casefabric.cmmn.instance.casefile.CaseFile;

import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public class CaseFileAPI extends APIObject<Case> {
    private final CaseFile file;
    private final Map<String, CaseFileItemAPI> items = new HashMap<>();

    public CaseFileAPI(CaseFile file) {
        super(file.getCaseInstance());
        this.file = file;
        this.file.getCaseFileItems().forEach(item -> {
            // Enable directly accessing the JSON structure of the CaseFileItem by name
            addPropertyReader(item.getName(), () -> new ValueAPI(item));
            // And enable CaseFileItem wrapper to be accessed by getItem() method
            CaseFileItemAPI itemAPI = new CaseFileItemAPI(item);
            items.put(item.getName(), itemAPI);
        });
    }

    public CaseFileItemAPI getItem(String name) {
        return items.get(name);
    }
}
