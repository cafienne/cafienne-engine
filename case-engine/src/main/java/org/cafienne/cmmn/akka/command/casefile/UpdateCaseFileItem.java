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
 * Updates content and/or properties of a case file item.
 */
@Manifest
public class UpdateCaseFileItem extends CaseFileItemCommand {
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
    public UpdateCaseFileItem(TenantUser tenantUser, String caseInstanceId, Path path, Value<?> newContent) {
        super(tenantUser, caseInstanceId, path, newContent, CaseFileItemTransition.Update);
    }

    public UpdateCaseFileItem(ValueMap json) {
        super(json, CaseFileItemTransition.Update);
    }

    @Override
    protected void apply(CaseFileItemCollection<?> caseFileItem, Value<?> content) {
        caseFileItem.updateContent(content);
    }
}
