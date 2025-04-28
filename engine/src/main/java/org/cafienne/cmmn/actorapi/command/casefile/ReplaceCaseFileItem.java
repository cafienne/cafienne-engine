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

package org.cafienne.cmmn.actorapi.command.casefile;

import org.cafienne.actormodel.identity.CaseUserIdentity;
import org.cafienne.cmmn.instance.Case;
import org.cafienne.cmmn.instance.casefile.CaseFileItemCollection;
import org.cafienne.cmmn.instance.casefile.CaseFileItemTransition;
import org.cafienne.cmmn.instance.Path;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.Value;
import org.cafienne.json.ValueMap;

/**
 * Replaces case file item content.
 */
@Manifest
public class ReplaceCaseFileItem extends CaseFileItemCommand {
    /**
     * Replaces the case file item content. Depending on the type, this will replace properties and/or content.
     * E.g., in the case of a JSONType, the existing contents of the case file item will be replaced with the new content, and the existing properties will
     * be replaced with new property values.<br/>
     * In addition, the engine will try to map any child content into child case file items, and additionally trigger the Replace transition on those children.
     *
     * @param caseInstanceId   The id of the case in which to perform this command.
     * @param newContent         A value structure with contents of the new case file item
     * @param path Path to the case file item to be created
     */
    public ReplaceCaseFileItem(CaseUserIdentity user, String caseInstanceId, String rootCaseId, Value<?> newContent, Path path) {
        super(user, caseInstanceId, rootCaseId, newContent, path, CaseFileItemTransition.Replace);
    }

    public ReplaceCaseFileItem(ValueMap json) {
        super(json, CaseFileItemTransition.Replace);
    }

    @Override
    void apply(Case caseInstance, CaseFileItemCollection<?> caseFileItem, Value<?> content) {
        caseFileItem.replaceContent(content);
    }
}
