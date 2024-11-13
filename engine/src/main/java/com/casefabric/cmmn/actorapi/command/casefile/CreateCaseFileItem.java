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

package com.casefabric.cmmn.actorapi.command.casefile;

import com.casefabric.actormodel.identity.CaseUserIdentity;
import com.casefabric.cmmn.instance.Case;
import com.casefabric.cmmn.instance.casefile.CaseFileItemCollection;
import com.casefabric.cmmn.instance.casefile.CaseFileItemTransition;
import com.casefabric.cmmn.instance.Path;
import com.casefabric.infrastructure.serialization.Manifest;
import com.casefabric.json.Value;
import com.casefabric.json.ValueMap;

/**
 * Creates a new case file item with certain content.
 */
@Manifest
public class CreateCaseFileItem extends CaseFileItemCommand {
    /**
     * Sets the case file item content. Depending on the type, this will fill both properties and/or content.
     * E.g., in the case of a JSONType, the existing contents of the case file item will be filled with the new content, and the existing properties will
     * be filled with new property values.
     * In addition, the engine will try to map any child content into child case file items, and additionally trigger the Create transition on those children.
     *
     * @param caseInstanceId   The id of the case in which to perform this command.
     * @param newContent         A value structure with contents of the new case file item
     * @param path Path to the case file item to be created
     */
    public CreateCaseFileItem(CaseUserIdentity user, String caseInstanceId, Value<?> newContent, Path path) {
        super(user, caseInstanceId, newContent, path, CaseFileItemTransition.Create);
    }

    public CreateCaseFileItem(ValueMap json) {
        super(json, CaseFileItemTransition.Create);
    }

    @Override
    void apply(Case caseInstance, CaseFileItemCollection<?> caseFileItem, Value<?> content) {
        caseFileItem.createContent(content);
    }
}
