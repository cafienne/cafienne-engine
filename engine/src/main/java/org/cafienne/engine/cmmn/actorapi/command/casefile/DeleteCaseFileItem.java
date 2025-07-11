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
 * Deletes a case file item.
 */
@Manifest
public class DeleteCaseFileItem extends CaseFileItemCommand {
    /**
     * Deletes the case file item.
     *
     * @param caseInstanceId   The id of the case in which to perform this command.
     * @param path Path to the case file item to be created
     */
    public DeleteCaseFileItem(CaseUserIdentity user, String caseInstanceId, Path path) {
        super(user, caseInstanceId, Value.NULL, path, CaseFileItemTransition.Delete);
    }

    public DeleteCaseFileItem(ValueMap json) {
        super(json, CaseFileItemTransition.Delete);
    }

    @Override
    void apply(Case caseInstance, CaseFileItemCollection<?> caseFileItem, Value<?> content) {
        caseFileItem.deleteContent();
    }
}
