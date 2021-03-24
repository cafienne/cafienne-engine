package org.cafienne.cmmn.expression.spel.api.cmmn.plan;

import org.cafienne.cmmn.expression.spel.api.APIObject;
import org.cafienne.cmmn.instance.Case;
import org.cafienne.cmmn.instance.PlanItem;

import java.util.HashMap;

/**
 *
 */
public class PlanItemAPI<P extends PlanItem> extends APIObject<Case> {
    protected final P item;

    private static HashMap<PlanItem, Integer> map = new HashMap();

    public String getName() {
        return item.getType().toLowerCase();
    }

    protected PlanItemAPI find(PlanItem item) {
        if (this.item == item) {
            return this;
        } else {
            return null;
        }
    }

    protected PlanItemAPI(P item, StageAPI stage) {
        super(item.getCaseInstance());
        this.item = item;
        addPropertyReader("id", () -> item.getId());
        addPropertyReader("name", () -> item.getName());
        addPropertyReader("index", () -> item.getIndex());
        addPropertyReader("state", () -> item.getState());
        addPropertyReader("stage", () -> stage);
    }

    public String getId() {
        warnDeprecation("getId()", "id");
        return item.getId();
    }
}
