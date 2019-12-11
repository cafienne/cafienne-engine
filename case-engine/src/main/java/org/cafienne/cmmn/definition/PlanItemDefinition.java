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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.Collection;

public class PlanItemDefinition extends CMMNElementDefinition {
    private final static Logger logger = LoggerFactory.getLogger(PlanItemDefinition.class);

    private ItemControlDefinition planItemControl;
    private PlanItemDefinitionDefinition definition;
    private final Collection<EntryCriterionDefinition> entryCriteria = new ArrayList<>();
    private final Collection<ExitCriterionDefinition> exitCriteria = new ArrayList<>();
    private final String planItemDefinitionRefValue;

    public PlanItemDefinition(Element element, Definition definition, CMMNElementDefinition parentElement) {
        super(element, definition, parentElement);
        this.planItemDefinitionRefValue = parseAttribute("definitionRef", true);

        parse("entryCriterion", EntryCriterionDefinition.class, this.entryCriteria);
        parse("exitCriterion", ExitCriterionDefinition.class, this.exitCriteria);
        planItemControl = parse("itemControl", ItemControlDefinition.class, false);
    }

    @Override
    public String toString() {
        return definition.getType() + "['" + getName() +"']";
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
            getCaseDefinition().addReferenceError("The plan item " + getName() +" refers to a definition named " + planItemDefinitionRefValue + ", but that definition is not found");
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
                    getCaseDefinition().addDefinitionError("The plan item " + getName() + " has exit sentries, but these are not allowed for a non blocking task");
                    return;
                }
            }
        }
    }

    private void resolvePlanItemDefinition() {
        this.definition = getCaseDefinition().findPlanItemDefinition(planItemDefinitionRefValue);
    }

}
