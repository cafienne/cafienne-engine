package org.cafienne.cmmn.definition.sentry;

import org.cafienne.cmmn.definition.CMMNElementDefinition;
import org.cafienne.cmmn.definition.ModelDefinition;
import org.cafienne.cmmn.definition.DiscretionaryItemDefinition;
import org.cafienne.cmmn.definition.PlanItemDefinition;
import org.cafienne.cmmn.instance.Transition;
import org.w3c.dom.Element;

public class EntryCriterionDefinition extends CriterionDefinition {
    /**
     * The transition to be invoked when this criterion becomes active.
     * Milestones and EventListeners trigger {@link Transition#Occur}.
     * Task and Stages trigger {@link Transition#Start}
     */
    private Transition entryTransition;

    public EntryCriterionDefinition(Element element, ModelDefinition modelDefinition, CMMNElementDefinition parentElement) {
        super(element, modelDefinition, parentElement);
    }

    /**
     * Only after resolving sentries we know what transition we need to make
     */
    @Override
    protected void resolveReferences() {
        super.resolveReferences();
        CMMNElementDefinition parent = getParentElement();
        if (parent instanceof PlanItemDefinition) {
            entryTransition = ((PlanItemDefinition) parent).getPlanItemDefinition().getEntryTransition();
        } else if (parent instanceof DiscretionaryItemDefinition) {
            entryTransition = ((DiscretionaryItemDefinition) parent).getPlanItemDefinition().getEntryTransition();
        } else {
            getCaseDefinition().addReferenceError(getContextDescription()+"Found an entry criterion inside a "+parent.getClass().getSimpleName()+", but that type is not supported for entry criteria");
        }
    }

    /**
     * Returns true if there is at least one on part in this definition
     * @return
     */
    public boolean hasOnParts() {
        // Check whether sentry definition exists at all, and if so, check whether it has on parts.
        return this.getSentryDefinition() != null && !getSentryDefinition().getOnParts().isEmpty();
    }

    @Override
    public Transition getTransition() {
        return entryTransition;
    }
}
