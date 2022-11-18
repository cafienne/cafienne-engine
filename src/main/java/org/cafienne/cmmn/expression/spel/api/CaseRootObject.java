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

package org.cafienne.cmmn.expression.spel.api;

import org.cafienne.cmmn.expression.spel.api.cmmn.plan.CaseAPI;
import org.cafienne.cmmn.expression.spel.api.cmmn.plan.PlanItemAPI;
import org.cafienne.cmmn.expression.spel.api.cmmn.team.MemberAPI;
import org.cafienne.cmmn.instance.Case;
import org.cafienne.cmmn.instance.PlanItem;

/**
 * Base class for giving object context to Case expressions
 * Exposes 'case' property.
 */
public abstract class CaseRootObject extends APIRootObject<Case> {
    private final CaseAPI context;

    protected CaseRootObject(Case model) {
        super(model, new MemberAPI(model.getCaseTeam(), model.getCurrentTeamMember()));
        this.context = new CaseAPI(model);
        addContextProperty(this.context, "case", "caseInstance");
    }

    protected Case getCase() {
        return getActor();
    }

    public CaseAPI getCaseInstance() {
        warnDeprecation("getCaseInstance()", "case");
        return context;
    }

    protected void registerPlanItem(PlanItem<?> item) {
        final PlanItemAPI<?> itemContext = this.context.find(item);
        addContextProperty(itemContext, itemContext.getName(), "planItem");
    }
}

