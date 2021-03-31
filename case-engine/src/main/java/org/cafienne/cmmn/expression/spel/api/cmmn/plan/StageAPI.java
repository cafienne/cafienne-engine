package org.cafienne.cmmn.expression.spel.api.cmmn.plan;

import org.cafienne.cmmn.definition.ItemDefinition;
import org.cafienne.cmmn.instance.*;

import java.util.*;

/**
 *
 */
public class StageAPI extends PlanItemAPI<Stage> {
    private final List<PlanItemAPI> children = new ArrayList();

    protected StageAPI(CaseAPI caseAPI, Stage stage, StageAPI parent) {
        super(caseAPI, stage, parent);
        Collection<PlanItem> items = stage.getPlanItems();
        Map<String, Object> itemAccessorsByName = new HashMap();
        for (PlanItem item : items) {
            PlanItemAPI childContext = createPlanItemContext(item);
            children.add(childContext);
            ItemDefinition itemDefinition = item.getItemDefinition();
            if (itemDefinition.getPlanItemControl().getRepetitionRule().isDefault()) {
                itemAccessorsByName.put(itemDefinition.getName(), childContext);
            } else {
                List list = (List) itemAccessorsByName.getOrDefault(itemDefinition.getName(), new ArrayList<PlanItemAPI>());
                list.add(childContext);
                itemAccessorsByName.put(itemDefinition.getName(), list);
            }
        }
        itemAccessorsByName.forEach((name, item) -> addPropertyReader(name, () -> item));
        addPropertyReader("items", itemAccessorsByName::values);
        addDeprecatedReader("planItems", "items", itemAccessorsByName::values);
    }

    private PlanItemAPI createPlanItemContext(PlanItem item) {
        if (item instanceof Stage) {
            return new StageAPI(caseAPI, (Stage) item, this);
        } else if (item instanceof Task) {
            return new TaskAPI(caseAPI, (Task) item, this);
        } else if (item instanceof Milestone) {
            return new MilestoneAPI(caseAPI, (Milestone) item, this);
        } else if (item instanceof TimerEvent) {
            return new TimerEventAPI(caseAPI, (TimerEvent) item, this);
        } else {
            // Hmmm... a not yet supported type of plan item? That's ok.
            return new PlanItemAPI(caseAPI, item, this);
        }
    }

    @Override
    public String getName() {
        return "stage";
    }

}
