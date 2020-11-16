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
 * Replaces case file item content.
 */
@Manifest
public class ReplaceCaseFileItem extends CaseFileItemCommand {
    /**
     * Replaces the case file item content. Depending on the type, this will replace properties and/or content.
     * E.g., in the case of a JSONType, the existing contents of the case file item will be replaced with the new content, and the existing properties will
     * be replaced with new property values.<br/>
     * In addition, the engine will try to map any child content into child case file items, and additionally trigger the Replace transition on those children.
     *
     * @param caseInstanceId The id of the case in which to perform this command.
     * @param path           Path to the case file item to be created
     * @param newContent     A value structure with contents of the new case file item
     */
    public ReplaceCaseFileItem(TenantUser tenantUser, String caseInstanceId, Path path, Value<?> newContent) {
        super(tenantUser, caseInstanceId, path, newContent, CaseFileItemTransition.Replace);
    }

    public ReplaceCaseFileItem(ValueMap json) {
        super(json, CaseFileItemTransition.Replace);
    }

    @Override
    protected void apply(CaseFileItemCollection<?> caseFileItem, Value<?> content) {
        caseFileItem.replaceContent(content);
    }
}
