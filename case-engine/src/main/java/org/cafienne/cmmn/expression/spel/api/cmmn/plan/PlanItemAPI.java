package org.cafienne.cmmn.expression.spel.api.cmmn.plan;

import org.cafienne.cmmn.expression.spel.api.APIObject;
import org.cafienne.cmmn.instance.Case;
import org.cafienne.cmmn.instance.PlanItem;

import java.util.HashMap;

/**
 *
 */
public class PlanItemAPI<P extends PlanItem<?>> extends APIObject<Case> {
    protected final P item;
    protected final StageAPI parent;
    protected final CaseAPI caseAPI;

    public String getName() {
        return item.getType().toLowerCase();
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
