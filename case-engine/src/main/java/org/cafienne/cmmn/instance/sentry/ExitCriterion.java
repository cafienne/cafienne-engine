package org.cafienne.cmmn.instance.sentry;

import org.cafienne.cmmn.definition.ItemDefinition;
import org.cafienne.cmmn.definition.sentry.ExitCriterionDefinition;
import org.cafienne.cmmn.instance.PlanItem;
import org.cafienne.cmmn.instance.Stage;
import org.cafienne.cmmn.instance.State;
import org.cafienne.cmmn.instance.Transition;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ExitCriterion extends Criterion<ExitCriterionDefinition> {
    private final List<PlanItem> targets = new ArrayList<>();

    public ExitCriterion(Stage stage, ExitCriterionDefinition definition) {
        super(stage, definition);
    }

    @Override
    public void addPlanItem(PlanItem planItem) {
        targets.add(planItem);
    }

    @Override
    protected void satisfy(OnPart<?, ?> activator) {
        final PlanItemOnPart piop = activator instanceof PlanItemOnPart ? (PlanItemOnPart) activator : null;
        final PlanItem source = piop != null ? piop.getSource() : null;

        List<PlanItem> killThese = targets.stream().filter(p -> p.getState() == State.Active).collect(Collectors.toList());
        killThese.forEach(planItem -> {
            // This checks whether the activating element does not accidentally belong to a sibling stage.
            //  Note: this is a slightly performance intensive operation, which is not necessary if the
            //  sentry network management does a better job.
            if (belongsToSiblingStage(source, findStage(planItem))) {
//                System.out.println("\n\nBELONGS TO SIBLING STAGE ...");
            } else {
                addDebugInfo(() -> "Exit criterion of '" + planItem + "' is satisfied", this.sentry);
                targets.remove(planItem);
                planItem.makeTransition(Transition.Exit);
            }
        });
    }

    private boolean belongsToSiblingStage(PlanItem source, Stage target) {
//        System.out.println("Checking whether " + source + " belongs to " + target);
        if (source == null) {
            return false;
        }
        if (source.getStage() == target) {
//            System.out.println("Well. Source is CHILD of target. That's a clear to go!");
            return false;
        }
        ItemDefinition sourceDefinition = source.getItemDefinition();
        ItemDefinition targetStageDefinition = target.getItemDefinition();
        if (sourceDefinition.equals(targetStageDefinition) && source != target) {
//            System.out.println("Well. Equal definitions, different instances. Must be from a sibling!!");
            return true;
        }

//        System.out.println("Not sure. checking parent");
        return belongsToSiblingStage(source.getStage(), target);

    }

    private Stage findStage(PlanItem target) {
        if (target instanceof Stage) return (Stage) target;
        return target.getStage();
    }

    @Override
    public boolean isEntryCriterion() {
        return false;
    }
}