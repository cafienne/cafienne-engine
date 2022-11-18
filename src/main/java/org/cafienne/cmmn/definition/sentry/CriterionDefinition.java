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

import org.cafienne.cmmn.definition.CMMNElementDefinition;
import org.cafienne.cmmn.definition.ModelDefinition;
import org.cafienne.cmmn.instance.Transition;
import org.cafienne.cmmn.instance.sentry.CriteriaListener;
import org.cafienne.cmmn.instance.sentry.Criterion;
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

    @Override
    protected boolean equalsWith(Object object) {
        return equalsWith(object, this::sameCriterion);
    }

    public boolean sameCriterion(CriterionDefinition other) {
        return same(sentry, other.sentry)
                && same(this.getTransition(), other.getTransition());
    }

    public abstract Criterion<?> createInstance(CriteriaListener<?,?> listener);
}
