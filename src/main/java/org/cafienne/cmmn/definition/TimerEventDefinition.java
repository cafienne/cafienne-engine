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

package org.cafienne.cmmn.definition;

import org.cafienne.cmmn.instance.*;
import org.w3c.dom.Element;

import java.time.Duration;
import java.time.Instant;

public class TimerEventDefinition extends EventListenerDefinition {
    private final ExpressionDefinition timerExpression;

    public TimerEventDefinition(Element element, ModelDefinition modelDefinition, CMMNElementDefinition parentElement) {
        super(element, modelDefinition, parentElement);
        timerExpression = parse("timerExpression", ExpressionDefinition.class, true);
    }

    @Override
    public PlanItemType getItemType() {
        return PlanItemType.TimerEvent;
    }

    /**
     * Returns the expression of the timer event.
     *
     * @return
     */
    public ExpressionDefinition getTimerExpression() {
        return timerExpression;
    }

    @Override
    public String getContextDescription() {
        String parentType = getParentElement().getType();
        String parentId = getParentElement().getId();
        // This will return something like "The parametermapping in HumanTask 'abc'
        return "The expression in " + parentType + " '" + parentId + "'";
    }

    public Instant getMoment(TimerEvent timerEvent) {
        Duration duration = timerExpression.getEvaluator().evaluateTimerExpression(timerEvent, this);
        Instant then = Instant.now().plus(duration);
        return then;
    }

    @Override
    public TimerEvent createInstance(String id, int index, ItemDefinition itemDefinition, Stage<?> stage, Case caseInstance) {
        return new TimerEvent(id, index, itemDefinition, this, stage);
    }

    @Override
    public Transition getEntryTransition() {
        return Transition.Occur;
    }

    @Override
    public boolean equalsWith(Object object) {
        return equalsWith(object, this::sameTimerEvent);
    }

    public boolean sameTimerEvent(TimerEventDefinition other) {
        return samePlanItemDefinitionDefinition(other)
                && same(timerExpression, other.timerExpression);
    }
}
