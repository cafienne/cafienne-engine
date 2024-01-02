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

package org.cafienne.cmmn.definition.sentry;

import org.cafienne.cmmn.definition.*;
import org.cafienne.cmmn.instance.Transition;
import org.cafienne.cmmn.instance.sentry.Criterion;
import org.cafienne.cmmn.instance.sentry.PlanItemOnPart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

public class PlanItemOnPartDefinition extends OnPartDefinition {
    private final static Logger logger = LoggerFactory.getLogger(PlanItemOnPartDefinition.class);

    private final Transition standardEvent;
    private final String sourceRef;
    private final String exitCriterionRef;
    private PlanItemDefinition source;
    private ExitCriterionDefinition exitCriterion;

    public PlanItemOnPartDefinition(Element element, ModelDefinition modelDefinition, CMMNElementDefinition parentElement) {
        super(element, modelDefinition, parentElement);
        String standardEventName = parseString("standardEvent", true);
        standardEvent = Transition.getEnum(standardEventName);
        if (standardEvent == null) {
            getCaseDefinition().addDefinitionError("A standard event named " + standardEventName + " does not exist for plan items");
        }
        sourceRef = parseAttribute("sourceRef", true);
        String sentryRef = parseAttribute("sentryRef", false);
        if (!sentryRef.isEmpty()) {
            logger.warn("Converting old sentry ref '" + sentryRef + "' in on part. Please upgrade the model.");
        }
        exitCriterionRef = parseAttribute("exitCriterionRef", false, sentryRef);
    }

    @Override
    protected void resolveReferences() {
        super.resolveReferences();
        // Add a check that the source only has to be filled if the standardEvent is specified
        source = getCaseDefinition().findPlanItem(this, sourceRef);
        if (source == null) {
            getCaseDefinition().addReferenceError("A plan item with name '" + sourceRef + "' is referenced from exitCriterion " + getParentElement().getName() + ", but it does not exist in the case plan model");
        }
        if (!exitCriterionRef.isEmpty()) {
            CMMNElementDefinition potentialCriterion = getCaseDefinition().getElement(exitCriterionRef);
            if (potentialCriterion instanceof ExitCriterionDefinition) {
                exitCriterion = (ExitCriterionDefinition) potentialCriterion;
            } else if (potentialCriterion instanceof SentryDefinition) {
                // old style model... ok let's support for now...
                exitCriterion = getCaseDefinition().findElement(e -> e instanceof ExitCriterionDefinition && ((ExitCriterionDefinition) e).getSentryDefinition() == potentialCriterion);
            }
            if (exitCriterion == null) {
                getCaseDefinition().addReferenceError("The exit criterion with name '" + exitCriterionRef + "' is referenced from the entry criterion " + getParentElement().getName() + " in plan item " + getParentElement().getName() + ", but it does not exist in the case plan model");
                return;
            }
            // Now add a check that the referenced exitCriterion is an Exit exitCriterion
            if (standardEvent != Transition.Exit) {
                getModelDefinition().addDefinitionError("The onPart in exitCriterion " + getParentElement().getName() + " must have 'exit' as its standard event, since it has a exitCriterionRef");
            }
        }
    }

    @Override
    public String getContextDescription() {
        return source.getPlanItemDefinition().getType() + "[" + source.getName() + "]." + standardEvent;
    }

    public Transition getStandardEvent() {
        return standardEvent;
    }

    @Override
    public PlanItemDefinition getSourceDefinition() {
        return source;
    }

    @Override
    public PlanItemOnPartDefinition asPlan() {
        return this;
    }

    public ExitCriterionDefinition getRelatedExitCriterion() {
        return exitCriterion;
    }

    @Override
    public PlanItemOnPart createInstance(Criterion<?> criterion) {
        return new PlanItemOnPart(criterion, this);
    }

    @Override
    public boolean equalsWith(Object object) {
        return equalsWith(object, this::sameOnPart);
    }

    public boolean sameOnPart(PlanItemOnPartDefinition other) {
        return same(standardEvent, other.standardEvent)
                && source.sameIdentifiers(other.source)
                && same(exitCriterion, other.exitCriterion);
    }
}