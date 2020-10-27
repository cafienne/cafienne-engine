/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.akka.command.casefile.file;

import org.cafienne.akka.actor.identity.TenantUser;
import org.cafienne.akka.actor.serialization.json.Value;
import org.cafienne.akka.actor.serialization.json.ValueMap;
import org.cafienne.cmmn.akka.command.casefile.CaseFileCommand;
import org.cafienne.cmmn.akka.command.response.CaseResponse;
import org.cafienne.cmmn.instance.Case;
import org.cafienne.cmmn.instance.casefile.CaseFile;
import org.cafienne.cmmn.instance.casefile.CaseFileItem;
import org.cafienne.cmmn.instance.casefile.CaseFileItemTransition;

/**
 * Holds some generic validation and processing behavior for CaseFile operations.
 */
abstract class CaseFileOperation extends CaseFileCommand {
    /**
     * Determine path and content for the CaseFileItem to be touched.
     *
     * @param caseInstanceId     The id of the case in which to perform this command.
     * @param newContent         A value structure with contents of the new case file item
     * @param intendedTransition
     */
    protected CaseFileOperation(TenantUser tenantUser, String caseInstanceId, Value<?> newContent, CaseFileItemTransition intendedTransition) {
        super(tenantUser, caseInstanceId, newContent, intendedTransition);
    }

    protected CaseFileOperation(ValueMap json, CaseFileItemTransition intendedTransition) {
        super(json, intendedTransition);
    }


    @Override
    public void validate(Case caseInstance) {
        // First do the validation in the super class. Then only our own, but also only if there were no validation errors from the super class.
        super.validate(caseInstance);

//        // Validate current state
        caseInstance.getCaseFile().validateTransition(intendedTransition, content);
//
//        // Validate type of new content
//        caseFileItem.getDefinition().validate(content);
    }

    @Override
    public CaseResponse process(Case caseInstance) {
        apply(caseInstance, caseInstance.getCaseFile(), content);
        return new CaseResponse(this);
    }

    abstract void apply(Case caseInstance, CaseFile caseFile, Value<?> content);

}
