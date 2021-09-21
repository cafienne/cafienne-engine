/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.definition;

import org.cafienne.cmmn.instance.Case;
import org.cafienne.cmmn.instance.Stage;
import org.cafienne.cmmn.instance.TimerEvent;
import org.cafienne.cmmn.instance.Transition;
import org.w3c.dom.Element;

import java.time.Duration;
import java.time.Instant;

public class TimerEventDefinition extends EventListenerDefinition {
    private final ExpressionDefinition timerExpression;

    public TimerEventDefinition(Element element, ModelDefinition modelDefinition, CMMNElementDefinition parentElement) {
        super(element, modelDefinition, parentElement);
        timerExpression = parse("timerExpression", ExpressionDefinition.class, true);
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
}
