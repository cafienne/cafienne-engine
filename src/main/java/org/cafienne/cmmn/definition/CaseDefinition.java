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

import org.cafienne.cmmn.definition.casefile.CaseFileDefinition;
import org.cafienne.cmmn.definition.casefile.CaseFileItemDefinition;
import org.cafienne.cmmn.definition.task.TaskImplementationContract;
import org.cafienne.cmmn.definition.team.CaseTeamDefinition;
import org.w3c.dom.Element;

import java.util.Collection;

/**
 * Parsed structure of a case definition (a top-level &lt;case&gt; element within a &lt;definitions&gt; document).
 */
public class CaseDefinition extends ModelDefinition implements TaskImplementationContract {
    private final CaseFileDefinition caseFileModel;
    private final CasePlanDefinition casePlanModel;
    private final CaseTeamDefinition caseTeamModel;

    public CaseDefinition(Element definitionElement, DefinitionsDocument document) {
        super(definitionElement, document);

        caseFileModel = parse("caseFileModel", CaseFileDefinition.class, true);
        casePlanModel = parse("casePlanModel", CasePlanDefinition.class, true);
        caseTeamModel = new CaseTeamDefinition(this);
    }

    /**
     * Returns the case plan of this definition.
     *
     * @return
     */
    public CasePlanDefinition getCasePlanModel() {
        return casePlanModel;
    }

    /**
     * Returns the case file model in this definition. The model is an abstraction on top of an actual object model.
     *
     * @return
     */
    public CaseFileDefinition getCaseFileModel() {
        return caseFileModel;
    }

    /**
     * Returns the case team model in this definition. Contains the roles and optionally some extensions.
     *
     * @return
     */
    public CaseTeamDefinition getCaseTeamModel() {
        return caseTeamModel;
    }

    /**
     * Searches the whole case plan for a PlanItem with the specified identifier.
     *
     * @param sourceLocation
     * @param planItemId
     * @return
     */
    public PlanItemDefinition findPlanItem(CMMNElementDefinition sourceLocation, String planItemId) {
        if (sourceLocation == null) {
            return null;
        }
        if (sourceLocation instanceof PlanFragmentDefinition) {
            PlanItemDefinition planItem = ((PlanFragmentDefinition) sourceLocation).searchPlanItem(planItemId);
            if (planItem != null) {
                return planItem;
            }
        }
        return findPlanItem(sourceLocation.getParentElement(), planItemId);
    }

    PlanItemDefinitionDefinition findPlanItemDefinition(String identifier) {
        Collection<PlanItemDefinitionDefinition> planItemDefinitions = getCasePlanModel().getPlanItemDefinitions();
        return planItemDefinitions.stream().filter(p -> p.hasIdentifier(identifier)).findFirst().orElse(null);
    }

    /**
     * Searches within the case file for a CaseFileItem with the specified name
     *
     * @param name
     * @return
     */
    public CaseFileItemDefinition findCaseFileItem(String name) {
        return getCaseFileModel().findCaseFileItem(name);
    }

    @Override
    public boolean equalsWith(Object object) {
        return equalsWith(object, this::sameCaseDefinition);
    }

    public boolean sameCaseDefinition(CaseDefinition other) {
        return sameModelDefinition(other)
                && same(caseFileModel, other.caseFileModel)
                && same(casePlanModel, other.casePlanModel)
                && same(caseTeamModel, other.caseTeamModel);
    }
}
