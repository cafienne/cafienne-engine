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

package com.casefabric.cmmn.definition.extension.workflow;

import com.casefabric.cmmn.definition.CMMNElementDefinition;
import com.casefabric.cmmn.definition.ItemDefinition;
import com.casefabric.cmmn.definition.ModelDefinition;
import org.w3c.dom.Element;

public class RendezVousDefinition extends TaskPairingDefinition {
    public RendezVousDefinition(Element element, ModelDefinition modelDefinition, CMMNElementDefinition parentElement) {
        super(element, modelDefinition, parentElement);
    }

    @Override
    protected RendezVousDefinition getCounterPart(ItemDefinition reference) {
        return reference.getRendezVousDefinition();
    }

    @Override
    protected void loadIndirectReferences() {
        fillChain(this);
    }

    private void fillChain(RendezVousDefinition definition) {
        definition.directReferences.stream()
            .filter(ItemDefinition::hasRendezVous) // All should be having rendez-vous, but still, let's ensure
            .filter(item -> !allReferences.contains(item)) // Avoid endless recursion
            .forEach(item -> { // Add the item, and also the ones it references
                allReferences.add(item);
                fillChain(item.getRendezVousDefinition());
            });
    }
}
