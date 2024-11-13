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

package org.cafienne.cmmn.expression.spel.api.cmmn.plan;

import org.cafienne.cmmn.expression.spel.api.APIObject;
import org.cafienne.cmmn.instance.Case;
import org.cafienne.cmmn.instance.PlanItem;

/**
 *
 */
public class PlanItemAPI<P extends PlanItem<?>> extends APIObject<Case> {
    protected final P item;
    protected final StageAPI parent;
    protected final CaseAPI caseAPI;

    public String getName() {
        return item.getType().toString().toLowerCase();
    }

    protected PlanItemAPI(CaseAPI caseAPI, P item, StageAPI parent) {
        super(item.getCaseInstance());
        this.item = item;
        this.caseAPI = caseAPI;
        this.parent = parent;
        addPropertyReader("id", item::getId);
        addPropertyReader("name", item::getName);
        addPropertyReader("index", item::getIndex);
        addPropertyReader("state", item::getState);
        addPropertyReader("stage", () -> parent);
        this.caseAPI.register(this);
    }

    public String getId() {
        warnDeprecation("getId()", "id");
        return item.getId();
    }

    protected String getPath() {
        String indexString = item.getItemDefinition().getPlanItemControl().getRepetitionRule().isDefault() ? "" : "." + item.getIndex();
        String pathString = "/" + item.getName() + indexString;
        return parent == null ? "CasePlan" : parent.getPath() + pathString;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + getPath() + "][" + item.getId() + "]";
    }
}
