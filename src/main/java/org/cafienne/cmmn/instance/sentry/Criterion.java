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

package org.cafienne.cmmn.instance.sentry;

import org.cafienne.cmmn.definition.XMLElementDefinition;
import org.cafienne.cmmn.definition.sentry.CaseFileItemOnPartDefinition;
import org.cafienne.cmmn.definition.sentry.CriterionDefinition;
import org.cafienne.cmmn.definition.sentry.OnPartDefinition;
import org.cafienne.cmmn.definition.sentry.PlanItemOnPartDefinition;
import org.cafienne.cmmn.instance.CMMNElement;
import org.cafienne.cmmn.instance.CasePlan;
import org.cafienne.cmmn.instance.PlanItem;
import org.cafienne.cmmn.instance.Stage;
import org.cafienne.cmmn.instance.casefile.CaseFileItem;
import org.cafienne.json.ValueList;
import org.cafienne.json.ValueMap;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class Criterion<D extends CriterionDefinition> extends CMMNElement<D> {
    private final CriteriaListener listener;
    private final Collection<OnPart<?,?,?>> onParts = new ArrayList<>();
    /**
     * Simple set to be able to quickly check whether the criterion may become active
     */
    private final Set<OnPart<?, ?, ?>> inactiveOnParts = new HashSet<>();
    /**
     * Flag switched when all on parts are active, and the if part also returned true
     * Gets de-activated when an on part becomes inactive.
     */
    private boolean isActive;

    protected Criterion(CriteriaListener listener, D definition) {
        super(listener.item, definition);
        this.listener = listener;
        for (OnPartDefinition onPartDefinition : getDefinition().getSentryDefinition().getOnParts()) {
            OnPart<?, ?, ?> onPart = onPartDefinition.createInstance(this);
            onParts.add(onPart);
            inactiveOnParts.add(onPart);
        }
        // Add ourselves to the sentry network
        getCaseInstance().getSentryNetwork().add(this);
        // Tell our onparts to connect to the case network
        onParts.forEach(OnPart::connectToCase);
    }

    public PlanItem<?> getTarget() {
        return listener.item;
    }

    public Stage<?> getStage() {
        return getTarget() instanceof CasePlan ? (CasePlan) getTarget() : getTarget().getStage();
    }

    private boolean evaluateIfPart() {
        addDebugInfo(() -> "Evaluating if part in " + this);
        boolean ifPartOutcome = getDefinition().getSentryDefinition().getIfPart().evaluate(this);
        // TODO: make sure to store the outcome of the ifpart evaluation?
        return ifPartOutcome;
    }

    /**
     * Whenever an on part is satisfied, it will try
     * to activate the Sentry.
     */
    void activate(OnPart<?, ?, ?> activator) {
        inactiveOnParts.remove(activator);
        if (inactiveOnParts.isEmpty()) {
            addDebugInfo(() -> this + " has become active", this.toJson());
        } else {
            addDebugInfo(() -> this + " has " + inactiveOnParts.size() + " remaining inactive on parts", this.toJson());
        }
        if (isSatisfied()) {
            isActive = true;
            listener.satisfy(this);
            // isActive = false;
        }
    }

    /**
     * If the on part is no longer satisfied,
     * it will dissatisfy the criterion too.
     */
    void deactivate(OnPart<?, ?, ?> activator) {
        isActive = false;
        inactiveOnParts.add(activator);
        addDebugInfo(() -> this + " now has " + inactiveOnParts.size() + " inactive on parts", this.toJson());
    }

    public boolean isActive() {
        return isActive;
    }

    public boolean isSatisfied() {
        // A Criterion is satisfied when one of the following conditions is satisfied:
        // 1 All of the onParts are satisfied AND the ifPart condition evaluates to "true".
        // 2 All of the onParts are satisfied AND there is no ifPart.
        // 3 The ifPart condition evaluates to "true" AND there are no onParts.

        if (onParts.isEmpty()) { // number 3
            return evaluateIfPart();
        } else { // numbers 1 and 2
            return inactiveOnParts.isEmpty() && evaluateIfPart();
        }
    }

    @Override
    public D getDefinition() {
        return super.getDefinition();
    }

    /**
     * Whether this is an activating or terminating criterion. This information is important for the order in which sentries are triggered.
     *
     */
    public abstract boolean isEntryCriterion();

    @Override
    public String toString() {
        boolean activated = isActive();

        String listeners = this.onParts.stream().map(OnPart::toString).collect(Collectors.joining(","));
        return getDefinition().getType() + " for " + getTarget() + " on " + "[" + listeners + "] - " + (activated ? "active" : "inactive");
    }

    /**
     * Connects to the plan item if there is an on part in the criterion that matches the plan item definition.
     * Skips plan items that belong to sibling stages.
     *
     */
    void establishPotentialConnection(PlanItem<?> planItem) {
        onParts.forEach(onPart -> onPart.establishPotentialConnection(planItem));
    }

    /**
     * Disconnect the item from our on parts, as the item is no longer part of the sentry network
     */
    void removeConnection(PlanItem<?> planItem) {
        onParts.forEach(onPart -> onPart.removeConnection(planItem));
    }

    /**
     * Connects to the case file item if there is an on part in the criterion that matches the case file item definition.
     */
    void establishPotentialConnection(CaseFileItem caseFileItem) {
        onParts.forEach(onPart -> onPart.establishPotentialConnection(caseFileItem));
    }

    /**
     * Disconnect the item from our on parts, as the item is no longer part of the sentry network
     */
    void removeConnection(CaseFileItem caseFileItem) {
        onParts.forEach(onPart -> onPart.removeConnection(caseFileItem));
    }

    public Element dumpMemoryStateToXML(Element parentElement, boolean showConnectedPlanItems) {
        Element sentryXML = parentElement.getOwnerDocument().createElement("Sentry");
        parentElement.appendChild(sentryXML);
        sentryXML.setAttribute("name", getDefinition().getName());
        sentryXML.setAttribute("id", getDefinition().getId());
        sentryXML.setAttribute("active", "" + inactiveOnParts.isEmpty());
        if (!showConnectedPlanItems) {
            sentryXML.setAttribute("target", getTarget().getPath() + "." + getDefinition().getTransition());
        } else {
            sentryXML.setAttribute("stage", getStage().getItemDefinition().getName());
        }

        this.onParts.forEach(onPart -> onPart.dumpMemoryStateToXML(sentryXML, showConnectedPlanItems));

        return sentryXML;
    }

    public ValueMap toJson() {
        ValueList onPartsJson = new ValueList();
        this.onParts.forEach(onPart -> onPartsJson.add(onPart.toJson()));
        return new ValueMap(
                "target", this.toString(),
                "active", inactiveOnParts.isEmpty(),
                "name", this.getDefinition().getName(),
                "id", this.getDefinition().getId(),
                "on-parts", onPartsJson);
    }

    public void release() {
        getCaseInstance().getSentryNetwork().remove(this);
        this.onParts.forEach(OnPart::releaseFromCase);
    }

    @Override
    public void migrateDefinition(D newDefinition, boolean skipLogic) {
        super.migrateDefinition(newDefinition, skipLogic);
        this.onParts.forEach(onPart -> {
            if (onPart instanceof CaseFileItemOnPart) {
                migrateCaseFileItemOnPart((CaseFileItemOnPart) onPart, newDefinition, skipLogic);
            } else if (onPart instanceof PlanItemOnPart) {
                migratePlanItemOnPart((PlanItemOnPart) onPart, newDefinition, skipLogic);
            }
        });
        addDebugInfo(() -> "Migrated " + this);
    }

    private void migrateCaseFileItemOnPart(CaseFileItemOnPart onPart, D newDefinition, boolean skipLogic) {
        CaseFileItemOnPartDefinition existingOnPartDefinition = onPart.getDefinition();
        CaseFileItemOnPartDefinition newOnPartDefinition = XMLElementDefinition.findDefinition(existingOnPartDefinition, newDefinition.getSentryDefinition().getOnParts());
        if (newOnPartDefinition != null) {
            onPart.migrateDefinition(newOnPartDefinition, skipLogic);
        } else {
            // perhaps search for a 'similar' on part, i.e. one with same source reference and potentially different standard event?
            // But then make sure that there is not another similar on part with the same source reference ;)
        }
    }

    private void migratePlanItemOnPart(PlanItemOnPart onPart, D newDefinition, boolean skipLogic) {
        PlanItemOnPartDefinition existingOnPartDefinition = onPart.getDefinition();
        PlanItemOnPartDefinition newOnPartDefinition = XMLElementDefinition.findDefinition(existingOnPartDefinition, newDefinition.getSentryDefinition().getOnParts());
        if (newOnPartDefinition != null) {
            onPart.migrateDefinition(newOnPartDefinition, skipLogic);
        }
    }
}