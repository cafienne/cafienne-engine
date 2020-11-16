/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.akka.command.casefile;

import org.cafienne.akka.actor.identity.TenantUser;
import org.cafienne.akka.actor.serialization.Manifest;
import org.cafienne.akka.actor.serialization.json.Value;
import org.cafienne.akka.actor.serialization.json.ValueMap;
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
     * @param caseInstanceId The id of the case in which to perform this command.
     * @param path           Path to the case file item to be created
     */
    public DeleteCaseFileItem(TenantUser tenantUser, String caseInstanceId, Path path) {
        super(tenantUser, caseInstanceId, path, Value.NULL, CaseFileItemTransition.Delete);
    }

    public DeleteCaseFileItem(ValueMap json) {
        super(json, CaseFileItemTransition.Delete);
    }

    @Override
    protected void apply(CaseFileItemCollection<?> caseFileItem, Value<?> content) {
        caseFileItem.deleteContent();
    }
}
