/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.definition;

import org.cafienne.cmmn.definition.sentry.EntryCriterionDefinition;
import org.cafienne.cmmn.definition.sentry.ExitCriterionDefinition;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.Collection;

public class PlanItemDefinition extends CMMNElementDefinition implements ItemDefinition {
    private ItemControlDefinition planItemControl;
    private PlanItemDefinitionDefinition definition;
    private final Collection<EntryCriterionDefinition> entryCriteria = new ArrayList<>();
    private final Collection<ExitCriterionDefinition> exitCriteria = new ArrayList<>();
    private final String planItemDefinitionRefValue;

    public PlanItemDefinition(Element element, ModelDefinition modelDefinition, CMMNElementDefinition parentElement) {
        super(element, modelDefinition, parentElement);
        this.planItemDefinitionRefValue = parseAttribute("definitionRef", true);

        parse("entryCriterion", EntryCriterionDefinition.class, this.entryCriteria);
        parse("exitCriterion", ExitCriterionDefinition.class, this.exitCriteria);
        planItemControl = parse("itemControl", ItemControlDefinition.class, false);
    }

    @Override
    public String toString() {
        return definition.getType() + "['" + getName() + "']";
    }

    public ItemControlDefinition getPlanItemControl() {
        return planItemControl;
    }

    public PlanItemDefinitionDefinition getPlanItemDefinition() {
        if (definition == null) {
            // Hmmm, apparently resolving is not yet done... (or it really cannot be found, in which case we again go search for it ;))
            resolvePlanItemDefinition();
        }
        return definition;
    }

    public Collection<EntryCriterionDefinition> getEntryCriteria() {
        return entryCriteria;
    }

    public Collection<ExitCriterionDefinition> getExitCriteria() {
        return exitCriteria;
    }

    @Override
    protected void resolveReferences() {
        super.resolveReferences();
        resolvePlanItemDefinition();
        if (this.definition == null) {
            getCaseDefinition().addReferenceError(getContextDescription() + " refers to a definition named " + planItemDefinitionRefValue + ", but that definition is not found");
            return; // Avoid further checking on this element
        }
        // If the plan item has no name, it has to be taken from the definition
        if (getName().isEmpty()) {
            setName(definition.getName());
        }
        if (planItemControl == null && this.definition != null) {
            // Create a default ItemControl
            planItemControl = this.definition.getDefaultControl();
        }

        // CMMN 1.0 spec page 23 says (yes indeed with a typo ...):
        // A PlanItem that is defined by a Task that is non-blocking (isBlocking set to "false") MUST NOT have exitCreteriaRefs.
        if (this.definition instanceof TaskDefinition) {
            if (!((TaskDefinition<?>) this.definition).isBlocking()) {
                if (!this.exitCriteria.isEmpty()) {
                    getCaseDefinition().addDefinitionError(getContextDescription() + " has exit sentries, but these are not allowed for a non blocking task");
                    return;
                }
            }
        }

        // CMMN 1.1 spec 5.4.11 page 53 says:
        //  A PlanItem that has a PlanItemControl that contains a RepetitionRule, MUST have either an entry criterion
        //   that refers to a Sentry that has at least one OnPart or no entry criteria at all.
        if (!this.planItemControl.getRepetitionRule().isDefault()) {
            if (this.entryCriteria.isEmpty()) {
                // Stages and Tasks are ok without entry criteria. But milestones must have entry criteria if they have a repetition rule
                if (this.definition instanceof MilestoneDefinition) {
                    getCaseDefinition().addDefinitionError(getContextDescription() + " has a repetition rule defined, but no entry criteria. This is mandatory.");
                }
            } else {
                // Check whether there is at least one entry criterion having an on part.
                if (this.getEntryCriteria().stream().noneMatch(EntryCriterionDefinition::hasOnParts)) {
                    getCaseDefinition().addDefinitionError(getContextDescription() + " has a repetition rule defined, but no entry criteria with at least one on part. This is mandatory.");
                }
            }
        }
    }

    @Override
    public String getContextDescription() {
        String type = this.definition != null ? this.definition.getType() : "Plan item";
        return type + " " + getName();
    }

    private void resolvePlanItemDefinition() {
        this.definition = getCaseDefinition().findPlanItemDefinition(planItemDefinitionRefValue);
    }

    @Override
    protected boolean equalsWith(Object object) {
        return equalsWith(object, this::samePlanItem);
    }

    public boolean samePlanItem(PlanItemDefinition other) {
        return sameIdentifiers(other)
                && same(this.planItemControl, other.planItemControl)
                && same(this.definition, other.definition)
                && same(this.entryCriteria, other.entryCriteria)
                && same(this.exitCriteria, other.exitCriteria);
    }
}
