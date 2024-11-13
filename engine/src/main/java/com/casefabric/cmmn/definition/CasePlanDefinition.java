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

package com.casefabric.cmmn.definition;

import com.casefabric.cmmn.definition.extension.workflow.FourEyesDefinition;
import com.casefabric.cmmn.definition.extension.workflow.RendezVousDefinition;
import com.casefabric.cmmn.definition.sentry.ExitCriterionDefinition;
import com.casefabric.cmmn.instance.Case;
import com.casefabric.cmmn.instance.CasePlan;
import com.casefabric.cmmn.instance.PlanItemType;
import com.casefabric.cmmn.instance.Stage;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.Collection;

public class CasePlanDefinition extends StageDefinition implements ItemDefinition {
    private final Collection<ExitCriterionDefinition> exitCriteria = new ArrayList<>(); // Only in the root stage

    public CasePlanDefinition(Element element, ModelDefinition modelDefinition, CMMNElementDefinition parentElement) {
        super(element, modelDefinition, parentElement);
        super.parse("exitCriterion", ExitCriterionDefinition.class, this.exitCriteria);
    }

    @Override
    public PlanItemType getItemType() {
        return PlanItemType.CasePlan;
    }

    @Override
    public PlanItemStarter getStarter() {
        return PlanItemStarter.isCasePlan(this);
    }

    @Override
    public CasePlan createInstance(String id, int index, ItemDefinition itemDefinition, Stage<?> stage, Case caseInstance) {
        return new CasePlan(id, this, caseInstance);
    }

    @Override
    public ItemControlDefinition getPlanItemControl() {
        return getDefaultControl();
    }

    @Override
    public PlanItemDefinitionDefinition getPlanItemDefinition() {
        return this;
    }

    @Override
    public Collection<ExitCriterionDefinition> getExitCriteria() {
        return exitCriteria;
    }

    @Override
    public FourEyesDefinition getFourEyesDefinition() {
        return null; // Should not exist
    }

    @Override
    public RendezVousDefinition getRendezVousDefinition() {
        return null; // Should not exist
    }

    @Override
    public boolean equalsWith(Object object) {
        return equalsWith(object, this::sameCasePlan);
    }

    public boolean sameCasePlan(CasePlanDefinition other) {
        return sameStage(other)
                && same(exitCriteria, other.exitCriteria);
    }
}
