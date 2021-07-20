package org.cafienne.cmmn.definition.sentry;

import org.cafienne.cmmn.definition.CMMNElementDefinition;
import org.cafienne.cmmn.definition.ModelDefinition;
import org.cafienne.cmmn.instance.Transition;
import org.w3c.dom.Element;

import java.util.stream.Collectors;

public abstract class CriterionDefinition extends CMMNElementDefinition {
    private final String sentryRef;
    private SentryDefinition sentry;

    protected CriterionDefinition(Element element, ModelDefinition modelDefinition, CMMNElementDefinition parentElement) {
        super(element, modelDefinition, parentElement);
        this.sentryRef = parseAttribute("sentryRef", true);
    }

    public SentryDefinition getSentryDefinition() {
        if (sentry == null) {
            // Sometimes this is invoked too early. Then try to resolve first
            this.resolveReferences();
        }
        return sentry;
    }

    public abstract Transition getTransition();

    @Override
    public String toString() {
        String onParts = getSentryDefinition().getOnParts().stream().map(OnPartDefinition::getContextDescription).collect(Collectors.joining(","));
        return getType() + " for " + getParentElement() + " on " + onParts;
    }

    /**
     * Returns the name of the plan item on which a transition has to be invoked when the criterion is satisfied
     *
     * @return
     */
    public String getTarget() {
        return this.getPlanItemName();
    }

    public String getPlanItemName() {
        return this.getParentElement().getName();
    }

    @Override
    protected void resolveReferences() {
        super.resolveReferences();
        this.sentry = getSurroundingStage().getSentry(sentryRef);
        if (this.sentry == null) {
            getCaseDefinition().addReferenceError("A sentry with name " + sentryRef + " is referenced from a plan item, but it cannot be found in the case plan");
        }
    }
}
