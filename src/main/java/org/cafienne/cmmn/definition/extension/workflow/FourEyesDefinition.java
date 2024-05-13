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

package org.cafienne.cmmn.definition.extension.workflow;

import org.cafienne.cmmn.definition.CMMNElementDefinition;
import org.cafienne.cmmn.definition.ItemDefinition;
import org.cafienne.cmmn.definition.ModelDefinition;
import org.w3c.dom.Element;

import java.util.List;
import java.util.stream.Collectors;

public class FourEyesDefinition extends TaskPairingDefinition {
    public FourEyesDefinition(Element element, ModelDefinition modelDefinition, CMMNElementDefinition parentElement) {
        super(element, modelDefinition, parentElement);
    }

    @Override
    protected FourEyesDefinition getCounterPart(ItemDefinition reference) {
        return reference.getFourEyesDefinition();
    }

    @Override
    protected void loadIndirectReferences() {
        directReferences.stream()
                .filter(ItemDefinition::hasFourEyes) // All should be having rendez-vous, but still, let's ensure
                .filter(item -> !allReferences.contains(item)) // Avoid endless recursion
                .forEach(item -> { // Add the item, and also the ones it references
                    allReferences.add(item);
                    // If one of our counterparts has rendez-vous, then we have also four eyes with all those elements the counterpart has rendez-vous with
                    if (item.hasRendezVous()) {
                        List<ItemDefinition> rendezVousItems = item.getRendezVousDefinition().getAllReferences().stream().filter(element -> !allReferences.contains(element)).collect(Collectors.toList());
                        allReferences.addAll(rendezVousItems);
                    }
                });
    }
}
