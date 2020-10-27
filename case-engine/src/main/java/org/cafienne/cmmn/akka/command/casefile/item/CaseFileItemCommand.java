/* 
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.akka.command.casefile.item;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.akka.actor.command.exception.InvalidCommandException;
import org.cafienne.akka.actor.identity.TenantUser;
import org.cafienne.akka.actor.serialization.Fields;
import org.cafienne.akka.actor.serialization.json.Value;
import org.cafienne.akka.actor.serialization.json.ValueMap;
import org.cafienne.cmmn.akka.command.casefile.CaseFileCommand;
import org.cafienne.cmmn.akka.command.response.CaseResponse;
import org.cafienne.cmmn.instance.Case;
import org.cafienne.cmmn.instance.casefile.CaseFileItem;
import org.cafienne.cmmn.instance.casefile.CaseFileItemTransition;
import org.cafienne.cmmn.instance.casefile.Path;

import java.io.IOException;

/**
 * Holds some generic validation and processing behavior for CaseFile operations.
 */
abstract class CaseFileItemCommand extends CaseFileCommand {
    protected final String caseFileItemPath;
    protected Path path;
    protected CaseFileItem caseFileItem;

    /**
     * Determine path and content for the CaseFileItem to be touched.
     *  @param caseInstanceId   The id of the case in which to perform this command.
     * @param newContent         A value structure with contents of the new case file item
     * @param caseFileItemPath Path to the case file item to be created
     * @param intendedTransition
     */
    protected CaseFileItemCommand(TenantUser tenantUser, String caseInstanceId, Value<?> newContent, String caseFileItemPath, CaseFileItemTransition intendedTransition) {
        super(tenantUser, caseInstanceId, newContent, intendedTransition);
        this.caseFileItemPath = caseFileItemPath;
    }

    protected CaseFileItemCommand(ValueMap json, CaseFileItemTransition intendedTransition) {
        super(json, intendedTransition);
        this.caseFileItemPath = readField(json, Fields.path);
    }

    @Override
    public void validate(Case caseInstance) {
        // First do the validation in the super class. Then only our own, but also only if there were no validation errors from the super class.
        super.validate(caseInstance);

        path = new Path(caseFileItemPath, caseInstance); // Creating the path validates it

        // Resolve the path on the case file
        caseFileItem = caseInstance.getCaseFile().getItem(path);

        // Validate current state
        caseFileItem.validateTransition(intendedTransition, content);

//        if (caseFileItem.validateTransition(intendedTransition, content)) {
//            throw new InvalidCommandException(getClass().getSimpleName() + "["+path+"] is not allowed in " + caseFileItem.getState() +" state.");
//        }
    }

    @Override
    public CaseResponse process(Case caseInstance) {
        apply(caseInstance, caseFileItem, content);
        return new CaseResponse(this);
    }

    abstract void apply(Case caseInstance, CaseFileItem caseFileItem, Value<?> content);

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + caseFileItemPath + "]";
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.write(generator);
        writeField(generator, Fields.path, caseFileItemPath);
    }
}
