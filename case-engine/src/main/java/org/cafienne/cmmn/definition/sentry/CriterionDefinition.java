package org.cafienne.cmmn.definition.sentry;

import org.cafienne.cmmn.definition.CMMNElementDefinition;
import org.cafienne.cmmn.definition.Definition;
import org.cafienne.cmmn.instance.Transition;
import org.w3c.dom.Element;

public abstract class CriterionDefinition extends CMMNElementDefinition {
    private final String sentryRef;
    private SentryDefinition sentry;

    protected CriterionDefinition(Element element, Definition definition, CMMNElementDefinition parentElement) {
        super(element, definition, parentElement);
        this.sentryRef = parseAttribute("sentryRef", true);
    }

    public SentryDefinition getSentryDefinition() {
        return sentry;
    }

    public abstract Transition getTransition();

    @Override
    public String toString() {
        return getType() + " for " + getParentElement();
    }

    /**
     * Returns the name of the plan item on which a transition has to be invoked when the sentry is satisfied
     *
     * @return
     */
    public String getTarget() {
        return this.getPlanItemName();
    }

    protected String getPlanItemName() {
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
