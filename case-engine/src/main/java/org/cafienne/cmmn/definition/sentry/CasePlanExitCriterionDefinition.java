/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.definition.sentry;

import org.cafienne.cmmn.definition.CMMNElementDefinition;
import org.cafienne.cmmn.definition.Definition;
import org.cafienne.cmmn.instance.sentry.CasePlanExitCriterion;
import org.cafienne.cmmn.instance.Stage;
import org.cafienne.cmmn.instance.Transition;
import org.w3c.dom.Element;

public class CasePlanExitCriterionDefinition extends ExitCriterionDefinition {
    public CasePlanExitCriterionDefinition(Element element, Definition definition, CMMNElementDefinition parentElement) {
        super(element, definition, parentElement);
    }

    @Override
    public String toString() {
        return getType();
    }

    @Override
    public String getTarget() {
        return null;
    }

    @Override
    public Transition getTransition() {
        return Transition.Terminate;
    }

    @Override
    public CasePlanExitCriterion createInstance(Stage<?> stage) {
        return new CasePlanExitCriterion(stage, this);
    }
}
