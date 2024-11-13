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

package com.casefabric.cmmn.instance.sentry;

import com.casefabric.cmmn.actorapi.event.file.CaseFileItemTransitioned;
import com.casefabric.cmmn.definition.sentry.CaseFileItemOnPartDefinition;
import com.casefabric.cmmn.instance.casefile.CaseFileItem;
import com.casefabric.cmmn.instance.casefile.CaseFileItemTransition;
import com.casefabric.json.ValueMap;
import org.w3c.dom.Element;

public class CaseFileItemOnPart extends OnPart<CaseFileItemOnPartDefinition, CaseFileItemTransitioned, CaseFileItem> {
    private boolean isActive;
    private CaseFileItemTransitioned lastEvent;

    public CaseFileItemOnPart(Criterion<?> criterion, CaseFileItemOnPartDefinition caseFileItemOnPartDefinition) {
        super(criterion, caseFileItemOnPartDefinition);
    }

    @Override
    CaseFileItemTransition getStandardEvent() {
        return getDefinition().getStandardEvent();
    }

    @Override
    void connectToCase() {
        // Try to connect with the case file item that is referenced from our definition
        CaseFileItem item = getDefinition().getSourceDefinition().getPath().resolve(getCaseInstance());
        establishPotentialConnection(item);
    }

    @Override
    public void releaseFromCase() {
        connectedItems.forEach(caseFileItem -> caseFileItem.releaseOnPart(this));
    }

    @Override
    protected void establishPotentialConnection(CaseFileItem caseFileItem) {
        if (connectedItems.contains(caseFileItem)) {
            // Avoid repeated additions
            return;
        }
        // Only connect if the case file item has the same definition as our source definition.
        if (!getDefinition().getSourceDefinition().equals(caseFileItem.getDefinition())) {
            return;
        }
        addDebugInfo(() -> "Connecting case file item " + caseFileItem + " to " + criterion);
        connectedItems.add(caseFileItem);
        caseFileItem.connectOnPart(this);
    }

    @Override
    protected void removeConnection(CaseFileItem caseFileItem) {
        connectedItems.remove(caseFileItem);
    }

    public void inform(CaseFileItem item, CaseFileItemTransitioned event) {
        addDebugInfo(() -> "Case file item " + item.getPath() + " informs " + criterion + " about transition " + event.getTransition() + ".");
        lastEvent = event;
        isActive = getStandardEvent().equals(event.getTransition());
        // Change in state...
        if (isActive) {
            criterion.activate(this);
        } else {
            criterion.deactivate(this);
        }
    }

    @Override
    ValueMap toJson() {
        return new ValueMap("casefile-item", getSourceName(),
                "active", isActive,
                "awaiting-transition", getStandardEvent(),
                "last-found-transition", "" + lastEvent
        );
    }

    @Override
    void dumpMemoryStateToXML(Element parentElement, boolean showConnectedPlanItems) {
        Element onPartXML = parentElement.getOwnerDocument().createElement("onPart");
        parentElement.appendChild(onPartXML);
        onPartXML.setAttribute("active", "" + isActive);
        onPartXML.setAttribute("source", getSourceName() + "." + getStandardEvent());
        onPartXML.setAttribute("last", "" + lastEvent);

        if (showConnectedPlanItems) {
            for (CaseFileItem caseFileItem : connectedItems) {
                String lastTransition = caseFileItem.getName() + "." + caseFileItem.getLastTransition();
                Element caseFileItemXML = parentElement.getOwnerDocument().createElement("caseFileItem");
                caseFileItemXML.setAttribute("last", lastTransition);
                caseFileItemXML.setAttribute("name", caseFileItem.getName());
                onPartXML.appendChild(caseFileItemXML);
            }
        }

    }
}
