/* 
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.actorapi.command.casefile;

import org.cafienne.actormodel.identity.TenantUser;
import org.cafienne.actormodel.serialization.Manifest;
import org.cafienne.json.Value;
import org.cafienne.json.ValueMap;
import org.cafienne.cmmn.instance.Case;
import org.cafienne.cmmn.instance.casefile.CaseFileItemCollection;
import org.cafienne.cmmn.instance.casefile.CaseFileItemTransition;
import org.cafienne.cmmn.instance.casefile.Path;

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
    public DeleteCaseFileItem(TenantUser tenantUser, String caseInstanceId, Path path) {
        super(tenantUser, caseInstanceId, Value.NULL, path, CaseFileItemTransition.Delete);
    }

    public DeleteCaseFileItem(ValueMap json) {
        super(json, CaseFileItemTransition.Delete);
    }

    @Override
    void apply(Case caseInstance, CaseFileItemCollection<?> caseFileItem, Value<?> content) {
        caseFileItem.deleteContent();
    }
}
