/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.definition;

import org.cafienne.cmmn.instance.Case;
import org.cafienne.cmmn.instance.PlanItemType;
import org.cafienne.cmmn.instance.Stage;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.Collection;

public class StageDefinition extends PlanFragmentDefinition {
    private final Collection<PlanItemDefinitionDefinition> planItemDefinitions = new ArrayList<>(); // Only in the root stage

    private final boolean autoComplete;
    private final PlanningTableDefinition planningTable;

    public StageDefinition(Element element, ModelDefinition modelDefinition, CMMNElementDefinition parentElement) {
        super(element, modelDefinition, parentElement);
        parse("humanTask", HumanTaskDefinition.class, planItemDefinitions);
        parse("processTask", ProcessTaskDefinition.class, planItemDefinitions);
        parse("caseTask", CaseTaskDefinition.class, planItemDefinitions);
        parse("milestone", MilestoneDefinition.class, planItemDefinitions);
        parse("userEvent", UserEventDefinition.class, planItemDefinitions);
        parse("timerEvent", TimerEventDefinition.class, planItemDefinitions);
        parse("stage", StageDefinition.class, planItemDefinitions);

        autoComplete = Boolean.parseBoolean(parseAttribute("autoComplete", false, "false"));
        planningTable = parse("planningTable", PlanningTableDefinition.class, false);

    }

    @Override
    public PlanItemType getItemType() {
        return PlanItemType.Stage;
    }

    public boolean autoCompletes() {
        return autoComplete;
    }

    public PlanningTableDefinition getPlanningTable() {
        return planningTable;
    }

    /**
     * Returns the definition of the discretionary item with the specified name - in this stage or it's human tasks (so not in one of it's children).
     *
     * @param identifier
     * @return
     */
    public DiscretionaryItemDefinition getDiscretionaryItem(String identifier) {
        DiscretionaryItemDefinition diDefinition = null;
        if (planningTable != null) {
            // Let's first check our stage-level planning table
            diDefinition = planningTable.getDiscretionaryItem(identifier);
        }
        if (diDefinition == null) {
            // Perhaps it is in one of our HumanTasks
            for (PlanItemDefinition planItem : getPlanItems()) {
                if (planItem.getPlanItemDefinition() instanceof HumanTaskDefinition) {
                    HumanTaskDefinition task = (HumanTaskDefinition) planItem.getPlanItemDefinition();
                    PlanningTableDefinition taskTable = task.getPlanningTable();
                    if (taskTable != null) {
                        diDefinition = task.getPlanningTable().getDiscretionaryItem(identifier);
                        if (diDefinition != null) {
                            break;
                        }
                    }
                }
            }
        }
        return diDefinition;
    }

    public Collection<PlanItemDefinitionDefinition> getPlanItemDefinitions() {
        return planItemDefinitions;
    }

    @Override
    public Stage<?> createInstance(String id, int index, ItemDefinition itemDefinition, Stage<?> stage, Case caseInstance) {
        return new Stage<>(id, index, itemDefinition, this, stage, caseInstance);
    }

    @Override
    protected boolean equalsWith(Object object) {
        return equalsWith(object, this::sameStage);
    }

    public boolean sameStage(StageDefinition other) {
        return samePlanFragment(other)
                && same(autoComplete, other.autoComplete)
                && same(planningTable, other.planningTable);
    }
}
