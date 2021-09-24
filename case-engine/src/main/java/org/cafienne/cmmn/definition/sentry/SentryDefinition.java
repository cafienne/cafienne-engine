/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.definition.sentry;

import org.cafienne.cmmn.definition.CMMNElementDefinition;
import org.cafienne.cmmn.definition.ModelDefinition;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.Collection;

public class SentryDefinition extends CMMNElementDefinition {
    private final Collection<OnPartDefinition> onParts = new ArrayList<>();
    private IfPartDefinition ifPart;

    public SentryDefinition(Element element, ModelDefinition definition, CMMNElementDefinition parentElement) {
        super(element, definition, parentElement, true);
        // XMLHelper.printXMLNode(element);
        this.ifPart = parse(IfPartDefinition.TAG_NAME, IfPartDefinition.class, false);
        parse("caseFileItemOnPart", CaseFileItemOnPartDefinition.class, onParts);
        parse("planItemOnPart", PlanItemOnPartDefinition.class, onParts);

        if (ifPart == null && onParts.isEmpty()) {
            getCaseDefinition().addDefinitionError("The sentry with id " + getId() + " and name " + getName() + " has no on parts and no if parts. It must have at least one on part or an if part");
        }

        if (ifPart == null) {
            // Create a default ifPart that always returns true
            ifPart = new IfPartDefinition(definition, this);
        }
    }

    public Collection<OnPartDefinition> getOnParts() {
        return onParts;
    }

    public IfPartDefinition getIfPart() {
        return ifPart;
    }

    @Override
    protected boolean equalsWith(Object object) {
        return equalsWith(object, this::sameSentry);
    }

    public boolean sameSentry(SentryDefinition other) {
        return same(ifPart, other.ifPart)
                && same(onParts, other.onParts);
    }
}