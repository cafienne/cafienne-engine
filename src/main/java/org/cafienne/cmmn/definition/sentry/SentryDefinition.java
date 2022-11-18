/*
 * Copyright (C) 2014  Batav B.V.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
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