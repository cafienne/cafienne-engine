/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.definition;

import org.cafienne.cmmn.instance.PlanItem;
import org.w3c.dom.Element;

public class ApplicabilityRuleDefinition extends ConstraintDefinition {
    public ApplicabilityRuleDefinition(Element element, ModelDefinition modelDefinition, CMMNElementDefinition parentElement) {
        super(element, modelDefinition, parentElement);
    }

    public boolean evaluate(PlanItem<?> planItem, ApplicabilityRuleDefinition rule, DiscretionaryItemDefinition discretionaryItemDefinition) {
        return getExpressionDefinition().getEvaluator().evaluateApplicabilityRule(planItem, discretionaryItemDefinition, rule);
    }

}