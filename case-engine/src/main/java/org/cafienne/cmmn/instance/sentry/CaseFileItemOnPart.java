/* 
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.instance.sentry;

import org.cafienne.cmmn.akka.event.debug.SentryEvent;
import org.cafienne.cmmn.definition.sentry.CaseFileItemOnPartDefinition;
import org.cafienne.cmmn.instance.CaseFileItem;
import org.cafienne.cmmn.instance.CaseFileItemTransition;
import org.cafienne.cmmn.instance.casefile.ValueMap;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.Collection;

public class CaseFileItemOnPart extends OnPart<CaseFileItemOnPartDefinition, CaseFileItem> {
    private Collection<CaseFileItem> connectedCaseFileItems = new ArrayList<CaseFileItem>();
    private final Object standardEvent;
    private final String sourceName;
    private boolean isActive;
    private CaseFileItemTransition lastTransition;

    public CaseFileItemOnPart(Sentry sentry, CaseFileItemOnPartDefinition caseFileItemOnPartDefinition) {
        super(sentry, caseFileItemOnPartDefinition);
        this.standardEvent = caseFileItemOnPartDefinition.getStandardEvent();
        this.sourceName = caseFileItemOnPartDefinition.getSource().getName();
    }

    @Override
    void connect(CaseFileItem caseFileItem) {
        addDebugInfo(SentryEvent.class, event -> event.addMessage("Connecting on part " + getDefinition().getId() + " to case file item " + caseFileItem, this.sentry));
        connectedCaseFileItems.add(caseFileItem);
        caseFileItem.connectOnPart(this);
    }

    // NOTE: this is basic, first implementation!
    //  yet to build up the experience with proper use case!! Especially update moments and the like.
    @Override
    public void inform(CaseFileItem caseFileItem) {
        addDebugInfo(SentryEvent.class, event -> event.addMessage("Case file item " + caseFileItem.getPath() + " informs " + sentry.criterion +" about transition " + caseFileItem.getLastTransition() + ".", this.sentry));
        lastTransition = caseFileItem.getLastTransition();
        boolean newActive = standardEvent.equals(lastTransition);
        if (isActive != newActive) {
            // Change in state...
            isActive = newActive;
            if (isActive) {
                sentry.activate(this);
            } else {
                sentry.deactivate(this);
            }
        }
    }

    @Override
    public String toString() {
        return sentry.toString() + ".on." + sourceName + "" + standardEvent;
    }

    @Override
    ValueMap toJson() {
        return new ValueMap("casefile-item", sourceName,
            "active", isActive,
            "awaiting-transition", standardEvent,
            "last-found-transition", lastTransition
        );
    }

    @Override
    Element dumpMemoryStateToXML(Element parentElement, boolean showConnectedPlanItems) {
        Element onPartXML = parentElement.getOwnerDocument().createElement("onPart");
        parentElement.appendChild(onPartXML);
        onPartXML.setAttribute("active", "" + isActive);
        onPartXML.setAttribute("source", sourceName + "." + standardEvent);
        onPartXML.setAttribute("last", sourceName + "." + lastTransition);

        if (showConnectedPlanItems) {
            for (CaseFileItem caseFileItem : connectedCaseFileItems) {
                String lastTransition = caseFileItem.getName() + "." + caseFileItem.getLastTransition();
                Element caseFileItemXML = parentElement.getOwnerDocument().createElement("caseFileItem");
                caseFileItemXML.setAttribute("last", lastTransition);
                caseFileItemXML.setAttribute("name", caseFileItem.getName());
                onPartXML.appendChild(caseFileItemXML);
            }
        }

        return onPartXML;
    }
}
