/* 
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.instance.sentry;

import org.cafienne.cmmn.definition.CMMNElementDefinition;
import org.cafienne.cmmn.definition.sentry.CaseFileItemOnPartDefinition;
import org.cafienne.cmmn.definition.sentry.OnPartDefinition;
import org.cafienne.cmmn.definition.sentry.PlanItemOnPartDefinition;
import org.cafienne.cmmn.definition.sentry.SentryDefinition;
import org.cafienne.cmmn.instance.*;
import org.cafienne.cmmn.instance.casefile.ValueList;
import org.cafienne.cmmn.instance.casefile.ValueMap;
import org.w3c.dom.Element;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class Sentry extends CMMNElement<SentryDefinition> {
    // On parts are stored by their source for easy lookup.
    // The source can only be a PlanItemDefinition or a CaseFileItemDefinition. We have taken the first
    // level parent class (CMMNElementDefinition) for this. So, technically we might also store on parts
    // with a different type of key ... but the logic prevents this from happening.
    private final Map<CMMNElementDefinition, OnPart<?, ?>> onParts = new LinkedHashMap<CMMNElementDefinition, OnPart<?, ?>>();

    /**
     * Simple set to be able to quickly check whether the sentry may become active
     */
    private final Set<OnPart<?, ?>> inactiveOnParts = new HashSet<OnPart<?, ?>>();

    /**
     * Stage that holds this sentry.
     */
    private final Stage stage;

    /**
     * Criterion that holds this sentry.
     */
    final Criterion criterion;

    /**
     * Indicates whether the sentry is currently being activated
     */
    private boolean isActive;

    Sentry(Stage stage, Criterion criterion) {
        super(stage, criterion.getDefinition().getSentryDefinition());
        this.stage = stage;
        this.criterion = criterion;
        for (OnPartDefinition onPartDefinition : getDefinition().getOnParts()) {
            if (onPartDefinition instanceof PlanItemOnPartDefinition) {
                createOnPart((PlanItemOnPartDefinition) onPartDefinition);
            } else {
                createOnPart((CaseFileItemOnPartDefinition) onPartDefinition);
            }
        }
        // Make ourselves known to the global case so that other plan items can start informing us.
        getCaseInstance().getSentryNetwork().add(this);
    }

    /**
     * Create a new on part, and connect it with the existing plan items in the
     * case.
     *
     * @param onPartDefinition
     */
    private void createOnPart(PlanItemOnPartDefinition onPartDefinition) {
        // A sentry that refers to us!
        PlanItemOnPart onPart = onPartDefinition.createInstance(this);
        onParts.put(onPartDefinition.getSource(), onPart);
        inactiveOnParts.add(onPart);

        for (PlanItem planItem : getCaseInstance().getPlanItems()) {
            if (onPartDefinition.getSource().equals(planItem.getItemDefinition())) {
                connect(planItem, onPart);
            }
        }
    }

    /**
     * Create a new on part, and connect it with the existing plan items in the
     * case.
     *
     * @param onPartDefinition
     */
    private void createOnPart(CaseFileItemOnPartDefinition onPartDefinition) {
        // A sentry that refers to us!
        CaseFileItemOnPart onPart = onPartDefinition.createInstance(this);
        onParts.put(onPartDefinition.getSource(), onPart);
        inactiveOnParts.add(onPart);

        CaseFile caseFile = getCaseInstance().getCaseFile();

        CaseFileItem item = caseFile.getItem(onPartDefinition.getSource().getPath());
        if (item != null) {
            item.iterator().forEachRemaining(innerItem -> connect(innerItem, onPart));
        }
    }

    /**
     * Connects the plan item to the on part that refers to it (if at all).
     * Skips plan items that belong to sibling stages.
     *
     * @param planItem
     */
    public void connect(PlanItem planItem) {
        PlanItemOnPart onPart = (PlanItemOnPart) onParts.get(planItem.getItemDefinition());
        if (onPart != null) {
            connect(planItem, onPart);
        }
    }

    /**
     * Connect the case file item to this sentry (if there is
     * an on part within this sentry referring to it).
     */
    public void connect(CaseFileItem caseFileItem) {
        CaseFileItemOnPart onPart = (CaseFileItemOnPart) onParts.get(caseFileItem.getDefinition());
        if (onPart != null) {
            connect(caseFileItem, onPart);
        }
    }

    /**
     * Connect the case file item with the on part
     *
     * @param caseFileItem
     * @param onPart
     */
    private void connect(CaseFileItem caseFileItem, CaseFileItemOnPart onPart) {
        addDebugInfo(() -> "Connecting case file item " + caseFileItem + " to " + criterion);
        onPart.connect(caseFileItem);
    }

    /**
     * Connect the plan item with the on part if the plan item does not belong
     * to a sibling stage
     *
     * @param planItem
     * @param onPart
     */
    private void connect(PlanItem planItem, PlanItemOnPart onPart) {
        if (doesNotBelongToSiblingStage(planItem)) {
            addDebugInfo(() -> "Connecting plan item " + planItem + " to " + criterion);
            onPart.connect(planItem);
        } else {
            addDebugInfo(() -> "Not connecting plan item " + planItem + " to " + criterion + " because it belongs to a sibling stage");
        }
    }

    /**
     * Determines whether the plan item belongs to the stage of this sentry or
     * to one of it's parents or descendants. If the plan item belongs to a
     * sibling stage, then it should not be connected to this sentry
     *
     * @param planItem
     * @return
     */
    private boolean doesNotBelongToSiblingStage(PlanItem planItem) {
        if (stage.contains(planItem)) {
            return true;
        }
        if (planItem.getStage().contains(stage)) {
            return true;
        }
        return false;
    }

    public boolean isSatisfied() {
        // A Sentry is satisfied when one of the following conditions is satisfied:
        // 1 All of the onParts are satisfied AND the ifPart condition evaluates to "true".
        // 2 All of the onParts are satisfied AND there is no ifPart.
        // 3 The ifPart condition evaluates to "true" AND there are no onParts.

        if (onParts.isEmpty()) { // number 3
            return evaluateIfPart();
        } else { // numbers 1 and 2
            return inactiveOnParts.isEmpty() && evaluateIfPart();
        }
    }

    private boolean evaluateIfPart() {
        addDebugInfo(() -> "Evaluating if part '"+getDefinition().getIfPart().getExpressionDefinition().getBody()+"'");
        boolean ifPartOutcome = getDefinition().getIfPart().evaluate(this);
        addDebugInfo(() -> "If part evaluation results in: " + ifPartOutcome);
        // TODO: make sure to store the outcome of the ifpart evaluation?
        return ifPartOutcome;
    }

    /**
     * Whenever an on part is satisfied, it will try
     * to activate the Sentry.
     */
    void activate(OnPart<?, ?> activator) {
        inactiveOnParts.remove(activator);
        if (inactiveOnParts.isEmpty()) {
            addDebugInfo(() -> criterion + " has become active.", this);
        } else {
            addDebugInfo(() -> criterion + " has "+inactiveOnParts.size()+" remaining inactive on parts", this);
        }
        if (isSatisfied()) {
            isActive = true;
            criterion.satisfy();
            // isActive = false;
        }
    }

    /**
     * If the on part is no longer satisfied,
     * it will dissatisfy the sentry too.
     */
    void deactivate(OnPart<?, ?> activator) {
        isActive = false;
        inactiveOnParts.add(activator);
        addDebugInfo(() -> criterion + " now has "+inactiveOnParts.size()+" inactive on parts", this);
    }

    public Element dumpMemoryStateToXML(Element parentElement, boolean showConnectedPlanItems) {
        Element sentryXML = parentElement.getOwnerDocument().createElement("Sentry");
        parentElement.appendChild(sentryXML);
        sentryXML.setAttribute("name", criterion.getDefinition().getName());
        sentryXML.setAttribute("id", criterion.getDefinition().getId());
        sentryXML.setAttribute("active", "" + inactiveOnParts.isEmpty());
        if (!showConnectedPlanItems) {
            String targetPlanItemName = criterion.getDefinition().getTarget();
            Transition targetTransition = criterion.getDefinition().getTransition();
            if (targetPlanItemName == null) {
                sentryXML.setAttribute("target", stage.getItemDefinition().getName() + "." + targetTransition);
            } else {
                sentryXML.setAttribute("target", targetPlanItemName + "." + targetTransition);
            }
        } else {
            sentryXML.setAttribute("stage", this.stage.getItemDefinition().getName());
        }

        this.onParts.forEach((d, onPart) -> {
            onPart.dumpMemoryStateToXML(sentryXML, showConnectedPlanItems);
        });

        return sentryXML;
    }

    public ValueMap toJson() {
        ValueList onPartsJson = new ValueList();
        this.onParts.forEach((d, onPart) -> onPartsJson.add(onPart.toJson()));
        return new ValueMap(
            "target", criterion.toString(),
            "active", inactiveOnParts.isEmpty(),
            "name", criterion.getDefinition().getName(),
            "id", criterion.getDefinition().getId(),
            "on-parts", onPartsJson);
    }

    public String toString() {
        return getDefinition().getName();
    }

    /**
     * Whether this is an activating or terminating sentry. This information is important for the order in which sentries are triggered.
     *
     * @return
     */
    public boolean isEntryCriterion() {
        return criterion.isEntryCriterion();
    }
    
    /**
     * The stage to which this sentry belongs
     *
     * @return
     */
    public Stage getStage() {
        return stage;
    }

    /**
     * Indicates whether the sentry is active or not
     * @return
     */
    public boolean isActive() {
        return isActive;
    }

    public Criterion getCriterion() {
        return criterion;
    }
}

