/* 
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.akka.command.casefile.file;

import org.cafienne.akka.actor.identity.TenantUser;
import org.cafienne.akka.actor.serialization.Manifest;
import org.cafienne.akka.actor.serialization.json.Value;
import org.cafienne.akka.actor.serialization.json.ValueMap;
import org.cafienne.cmmn.instance.Case;
import org.cafienne.cmmn.instance.casefile.CaseFile;
import org.cafienne.cmmn.instance.casefile.CaseFileItemTransition;

/**
 * Creates a new case file item with certain content.
 */
@Manifest
public class DeleteCaseFile extends CaseFileOperation {
    private static final CaseFileItemTransition transition = CaseFileItemTransition.Delete;

    /**
     * Sets the case file item content. Depending on the type, this will fill both properties and/or content.
     * E.g., in the case of a JSONType, the existing contents of the case file item will be filled with the new content, and the existing properties will
     * be filled with new property values.
     * In addition, the engine will try to map any child content into child case file items, and additionally trigger the Create transition on those children.
     *
     * @param caseInstanceId   The id of the case in which to perform this command.
     */
    public DeleteCaseFile(TenantUser tenantUser, String caseInstanceId) {
        super(tenantUser, caseInstanceId, Value.NULL, transition);
    }

    public DeleteCaseFile(ValueMap json) {
        super(json, transition);
    }

    @Override
    void apply(Case caseInstance, CaseFile caseFile, Value<?> content) {
        caseFile.deleteContent();
    }
}
