/* 
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.akka.command.casefile;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.akka.actor.command.exception.InvalidCommandException;
import org.cafienne.akka.actor.identity.TenantUser;
import org.cafienne.akka.actor.serialization.Fields;
import org.cafienne.cmmn.akka.command.CaseCommand;
import org.cafienne.cmmn.akka.command.response.CaseResponse;
import org.cafienne.cmmn.instance.Case;
import org.cafienne.cmmn.instance.CaseFileItem;
import org.cafienne.cmmn.instance.Path;
import org.cafienne.cmmn.instance.casefile.Value;
import org.cafienne.cmmn.instance.casefile.ValueMap;

import java.io.IOException;

/**
 * Holds some generic validation and processing behavior for CaseFile operations.
 */
abstract class CaseFileItemCommand extends CaseCommand {
    protected final Value<?> content;
    protected final String caseFileItemPath;
    protected Path path;
    protected CaseFileItem caseFileItem;

    /**
     * Determine path and content for the CaseFileItem to be touched.
     *
     * @param caseInstanceId   The id of the case in which to perform this command.
     * @param newContent         A value structure with contents of the new case file item
     * @param caseFileItemPath Path to the case file item to be created
     */
    protected CaseFileItemCommand(TenantUser tenantUser, String caseInstanceId, Value<?> newContent, String caseFileItemPath) {
        super(tenantUser, caseInstanceId);
        this.caseFileItemPath = caseFileItemPath;
        this.content = newContent;
    }

    protected CaseFileItemCommand(ValueMap json) {
        super(json);
        this.content = readMap(json, Fields.content);
        this.caseFileItemPath = readField(json, Fields.path);
    }

    @Override
    public void validate(Case caseInstance) {
        // First do the validation in the super class. Then only our own, but also only if there were no validation errors from the super class.
        super.validate(caseInstance);

        path = new Path(caseFileItemPath, caseInstance); // Creating the path validates it

        // Resolve the path on the case file
        caseFileItem = caseInstance.getCaseFile().getItem(path);
        caseFileItem.getDefinition().validate(content);
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
        writeField(generator, Fields.content, content);
        writeField(generator, Fields.path, caseFileItemPath);
    }
}
