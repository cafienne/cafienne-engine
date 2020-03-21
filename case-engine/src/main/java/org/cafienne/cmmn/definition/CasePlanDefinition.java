/* 
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.definition;

import org.cafienne.cmmn.definition.sentry.CasePlanExitCriterionDefinition;
import org.cafienne.cmmn.definition.sentry.EntryCriterionDefinition;
import org.cafienne.cmmn.definition.sentry.ExitCriterionDefinition;
import org.cafienne.cmmn.instance.Case;
import org.cafienne.cmmn.instance.CasePlan;
import org.cafienne.cmmn.instance.Stage;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.Collection;

public class CasePlanDefinition extends StageDefinition implements ItemDefinition {
    private final Collection<ExitCriterionDefinition> exitCriteria = new ArrayList<>(); // Only in the root stage

    public CasePlanDefinition(Element element, Definition definition, CMMNElementDefinition parentElement) {
        super(element, definition, parentElement);
        super.parse("exitCriterion", CasePlanExitCriterionDefinition.class, this.exitCriteria);
    }

    @Override
    public CasePlan createInstance(String id, int index, ItemDefinition itemDefinition, Stage stage, Case caseInstance) {
        return new CasePlan(id, this, caseInstance);
    }

    @Override
    public ItemControlDefinition getPlanItemControl() {
        throw new NullPointerException("CasePlanDefinition does not have item control; it should not be asked for...");
    }

    @Override
    public PlanItemDefinitionDefinition getPlanItemDefinition() {
        return this;
    }

    @Override
    public Collection<ExitCriterionDefinition> getExitCriteria() {
        return exitCriteria;
    }
}
