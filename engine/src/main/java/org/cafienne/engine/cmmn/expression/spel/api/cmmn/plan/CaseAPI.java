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

package org.cafienne.engine.cmmn.expression.spel.api.cmmn.plan;

import org.cafienne.engine.cmmn.expression.spel.api.APIObject;
import org.cafienne.engine.cmmn.expression.spel.api.cmmn.file.CaseFileAPI;
import org.cafienne.engine.cmmn.expression.spel.api.cmmn.team.CaseTeamAPI;
import org.cafienne.engine.cmmn.instance.Case;
import org.cafienne.engine.cmmn.instance.PlanItem;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class CaseAPI extends APIObject<Case> {
    private final StageAPI casePlan;
    private final CaseTeamAPI caseTeam;
    private final CaseFileAPI caseFile;

    private final Map<String, PlanItemAPI<?>> planItems = new HashMap<>();

    public CaseAPI(Case actor) {
        super(actor);
        this.casePlan = new StageAPI(this, actor.getCasePlan(), null);
        this.caseTeam = new CaseTeamAPI(actor.getCaseTeam());
        this.caseFile = new CaseFileAPI(actor.getCaseFile());
        addPropertyReader("plan", () -> casePlan);
        addPropertyReader("file", () -> caseFile);
        addPropertyReader("team", () -> caseTeam);
        addPropertyReader("id", actor::getId);
        addPropertyReader("name", () -> actor.getDefinition().getName());
        addPropertyReader("tenant", actor::getTenant);
        addPropertyReader("parent", actor::getParentCaseId);
        addPropertyReader("root", actor::getRootCaseId);
        addPropertyReader("createdOn", actor::getCreatedOn);
        addPropertyReader("lastModified", actor::getLastModified);
        addDeprecatedReader("caseFile", "file", () -> caseFile);
    }

    protected void register(PlanItemAPI<?> item) {
        planItems.put(item.item.getId(), item);
    }

    public String getId() {
        warnDeprecation("getId()", "id");
        return actor.getId();
    }

    public Collection<PlanItem<?>> getPlanItems(String identifier) {
        warnDeprecation("getPlanItemByName(\"" + identifier + "\")", "plan");
        return actor.getPlanItems(identifier);
    }

    public PlanItemAPI<?> getPlanItemByName(String identifier) {
        warnDeprecation("getPlanItemByName(\"" + identifier + "\")", "plan");
        return find(actor.getPlanItemByName(identifier));
    }

    public CaseTeamAPI getCaseTeam() {
        warnDeprecation("getCaseTeam()", "team");
        return caseTeam;
    }

    public CaseFileAPI getCaseFile() {
        warnDeprecation("getCaseFile()", "file");
        return caseFile;
    }

    public PlanItemAPI<?> find(PlanItem<?> item) {
        PlanItemAPI<?> api = planItems.get(item.getId());
        if (api == null) {
            getActor().addDebugInfo(() -> "ERROR: Unexpectedly cannot find a PlanItemAPI object for " + item);
        }
        return api;
    }
}
