/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.actorapi.command.casefile;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.akka.actor.identity.TenantUser;
import org.cafienne.akka.actor.serialization.Fields;
import org.cafienne.akka.actor.serialization.json.Value;
import org.cafienne.akka.actor.serialization.json.ValueMap;
import org.cafienne.cmmn.actorapi.command.CaseCommand;
import org.cafienne.cmmn.actorapi.response.CaseResponse;
import org.cafienne.cmmn.instance.Case;
import org.cafienne.cmmn.instance.casefile.CaseFileItemCollection;
import org.cafienne.cmmn.instance.casefile.CaseFileItemTransition;
import org.cafienne.cmmn.instance.casefile.Path;

import java.io.IOException;

/**
 * Holds some generic validation and processing behavior for CaseFile operations.
 */
abstract class CaseFileItemCommand extends CaseCommand {
    protected Path path;
    protected final Value<?> content;
    protected final CaseFileItemTransition intendedTransition;
    protected CaseFileItemCollection<?> caseFileItem;

    /**
     * Determine path and content for the CaseFileItem to be touched.
     *
     * @param caseInstanceId     The id of the case in which to perform this command.
     * @param newContent         A value structure with contents of the new case file item
     * @param path   Path to the case file item to be created
     * @param intendedTransition
     */
    protected CaseFileItemCommand(TenantUser tenantUser, String caseInstanceId, Value<?> newContent, Path path, CaseFileItemTransition intendedTransition) {
        super(tenantUser, caseInstanceId);
        this.path = path;
        this.content = newContent;
        this.intendedTransition = intendedTransition;
    }

    protected CaseFileItemCommand(ValueMap json, CaseFileItemTransition intendedTransition) {
        super(json);
        this.path = readPath(json, Fields.path);
        this.content = json.get(Fields.content.toString());
        this.intendedTransition = intendedTransition;
    }

    @Override
    public void validate(Case caseInstance) {
        // First do the validation in the super class. Then only our own, but also only if there were no validation errors from the super class.
        super.validate(caseInstance);
        // Resolve the path on the case file, this validates whether the path matches the case file definition
        caseFileItem = path.resolve(caseInstance);
        // Validate current item state against intended operation
        caseFileItem.validateTransition(intendedTransition, content);
    }

    @Override
    public CaseResponse process(Case caseInstance) {
        apply(caseInstance, caseFileItem, content);
        return new CaseResponse(this);
    }

    abstract void apply(Case caseInstance, CaseFileItemCollection<?> caseFileItem, Value<?> content);

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + path + "]";
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.write(generator);
        writeField(generator, Fields.path, path);
        writeField(generator, Fields.content, content);
    }
}
