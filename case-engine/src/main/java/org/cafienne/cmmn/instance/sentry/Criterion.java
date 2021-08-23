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
            addDebugInfo(() -> this + " has become active", this);
        } else {
            addDebugInfo(() -> this + " has " + inactiveOnParts.size() + " remaining inactive on parts", this);
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
        addDebugInfo(() -> this + " now has " + inactiveOnParts.size() + " inactive on parts", this);
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
     * Connects to the case file item if there is an on part in the criterion that matches the case file item definition.
     */
    void establishPotentialConnection(CaseFileItem caseFileItem) {
        onParts.forEach(onPart -> onPart.establishPotentialConnection(caseFileItem));
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
    public void migrateDefinition(D newDefinition) {
        getTarget().MigDevConsole("Migrating " + newDefinition.getType() +" for item " + getTarget().getName());
        super.migrateDefinition(newDefinition);
        this.onParts.forEach(onPart -> {
            if (onPart instanceof CaseFileItemOnPart) {
                migrateCaseFileItemOnPart((CaseFileItemOnPart) onPart, newDefinition);
            } else if (onPart instanceof PlanItemOnPart) {
                migratePlanItemOnPart((PlanItemOnPart) onPart, newDefinition);
            }
        });
    }

    private void migrateCaseFileItemOnPart(CaseFileItemOnPart onPart, D newDefinition) {
        CaseFileItemOnPartDefinition existingOnPartDefinition = onPart.getDefinition();
        CaseFileItemOnPartDefinition newOnPartDefinition = XMLElementDefinition.findDefinition(existingOnPartDefinition, newDefinition.getSentryDefinition().getOnParts());
        if (newOnPartDefinition != null) {
            getTarget().MigDevConsole("Migrating CaseFileItemOnPart in " + newDefinition.getType() +" for item " + getTarget().getName());
            onPart.migrateDefinition(newOnPartDefinition);
        } else {
            // perhaps search for a 'similar' on part, i.e. one with same source reference and potentially different standard event?
            // But then make sure that there is not another similar on part with the same source reference ;)
        }
    }

    private void migratePlanItemOnPart(PlanItemOnPart onPart, D newDefinition) {
        PlanItemOnPartDefinition existingOnPartDefinition = onPart.getDefinition();
        PlanItemOnPartDefinition newOnPartDefinition = XMLElementDefinition.findDefinition(existingOnPartDefinition, newDefinition.getSentryDefinition().getOnParts());
        if (newOnPartDefinition != null) {
            getTarget().MigDevConsole("Migrating PlanItemOnPart in " + newDefinition.getType() +" for item " + getTarget().getName());
            onPart.migrateDefinition(newOnPartDefinition);
        }
    }
}