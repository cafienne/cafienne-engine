/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.akka.command.casefile.document;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.akka.actor.command.exception.InvalidCommandException;
import org.cafienne.akka.actor.identity.TenantUser;
import org.cafienne.akka.actor.serialization.Fields;
import org.cafienne.akka.actor.serialization.Manifest;
import org.cafienne.akka.actor.serialization.json.Value;
import org.cafienne.akka.actor.serialization.json.ValueMap;
import org.cafienne.cmmn.akka.command.casefile.CaseFileItemCommand;
import org.cafienne.cmmn.instance.casefile.*;
import org.cafienne.cmmn.instance.casefile.document.StorageResult;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Updates content and/or properties of a case file item.
 */
@Manifest
public class AddDocumentInformation extends CaseFileItemCommand {
    private final List<StorageResult> storageResult;

    /**
     * Updates the case file item content. Depending on the type, this may merge properties and/or content with existing properties and content.
     * E.g., in the case of a JSONType, the existing contents of the case file item will be merged with the new content, and the existing properties will
     * be updated with new property values. <br/>
     * In addition, the engine will try to map any child content into child case file items, and additionally trigger the Update or Create transition on those children.
     *
     * @param caseInstanceId The id of the case in which to perform this command.
     * @param path           Path to the case file item to be created
     * @param newContent     A value structure with contents of the new case file item
     */
    public AddDocumentInformation(TenantUser tenantUser, String caseInstanceId, Path path, Value<?> newContent, StorageResult[] storageResult) {
        super(tenantUser, caseInstanceId, path, newContent, CaseFileItemTransition.Create);
        this.storageResult = Arrays.asList(storageResult);
    }

    public AddDocumentInformation(ValueMap json) {
        super(json, CaseFileItemTransition.Create);
        this.storageResult = readList(json, Fields.storageResult, v -> StorageResult.deserialize(v));
    }

    @Override
    protected void validate(CaseFileItemCollection<?> item) {
        if (item instanceof CaseFile) {
            throw new InvalidCommandException("Cannot upload documents directly into the CaseFile; please specify a CaseFileItem path");
        }
        if (!((CaseFileItem) item).getDefinition().getCaseFileItemDefinition().getDefinitionType().isDocument()) {
            throw new InvalidCommandException("Cannot upload documents to CaseFileItem " + path);
        }
    }

    @Override
    protected void apply(CaseFileItemCollection<?> caseFileItem, Value<?> content) {
        caseFileItem.addStorageResult(content, storageResult);
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.write(generator);
        writeListField(generator, Fields.storageResult, storageResult);
    }
}
