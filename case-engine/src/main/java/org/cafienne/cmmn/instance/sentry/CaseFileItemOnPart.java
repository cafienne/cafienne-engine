/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.instance.sentry;

import org.cafienne.cmmn.actorapi.event.file.CaseFileItemTransitioned;
import org.cafienne.cmmn.definition.sentry.CaseFileItemOnPartDefinition;
import org.cafienne.cmmn.instance.casefile.CaseFileItem;
import org.cafienne.cmmn.instance.casefile.CaseFileItemTransition;
import org.cafienne.json.ValueMap;
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
