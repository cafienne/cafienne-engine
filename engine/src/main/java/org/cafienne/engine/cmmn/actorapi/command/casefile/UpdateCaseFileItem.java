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

package org.cafienne.engine.cmmn.actorapi.command.casefile;

import org.cafienne.actormodel.identity.CaseUserIdentity;
import org.cafienne.engine.cmmn.instance.Case;
import org.cafienne.engine.cmmn.instance.casefile.CaseFileItemCollection;
import org.cafienne.engine.cmmn.instance.casefile.CaseFileItemTransition;
import org.cafienne.engine.cmmn.instance.Path;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.Value;
import org.cafienne.json.ValueMap;

/**
 * Updates content and/or properties of a case file item.
 */
@Manifest
public class UpdateCaseFileItem extends CaseFileItemCommand {
    /**
     * Updates the case file item content. Depending on the type, this may merge properties and/or content with existing properties and content.
     * E.g., in the case of a JSONType, the existing contents of the case file item will be merged with the new content, and the existing properties will
     * be updated with new property values. <br/>
     * In addition, the engine will try to map any child content into child case file items, and additionally trigger the Update or Create transition on those children.
     *
     * @param caseInstanceId   The id of the case in which to perform this command.
     * @param newContent         A value structure with contents of the new case file item
     * @param path Path to the case file item to be created
     */
    public UpdateCaseFileItem(CaseUserIdentity user, String caseInstanceId, Value<?> newContent, Path path) {
        super(user, caseInstanceId, newContent, path, CaseFileItemTransition.Update);
    }

    public UpdateCaseFileItem(ValueMap json) {
        super(json, CaseFileItemTransition.Update);
    }

    @Override
    void apply(Case caseInstance, CaseFileItemCollection<?> caseFileItem, Value<?> content) {
        caseFileItem.updateContent(content);
    }
}
