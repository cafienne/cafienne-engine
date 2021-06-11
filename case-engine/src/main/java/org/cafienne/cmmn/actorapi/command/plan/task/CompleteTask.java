/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.actorapi.command.plan.task;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.actormodel.ModelActor;
import org.cafienne.actormodel.command.exception.InvalidCommandException;
import org.cafienne.infrastructure.serialization.Fields;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.cmmn.actorapi.command.CaseCommand;
import org.cafienne.cmmn.actorapi.command.plan.MakePlanItemTransition;
import org.cafienne.cmmn.actorapi.response.CaseResponse;
import org.cafienne.cmmn.instance.Case;
import org.cafienne.cmmn.instance.PlanItem;
import org.cafienne.cmmn.instance.Task;
import org.cafienne.cmmn.instance.Transition;
import org.cafienne.json.ValueMap;

import java.io.IOException;

/**
 * This command can be used to complete a task with additional task output parameters. An alternative is to use the {@link MakePlanItemTransition}
 * with {@link Transition}.Complete. However, that does not allow for passing output parameters, as it is generic across Tasks, Stages, Milestones and
 * Events.
 */
@Manifest
public class CompleteTask extends CaseCommand {
    protected final String taskId;
    protected final ValueMap taskOutput;
    protected Task<?> task;

    /**
     * Create a command to transition the plan item with the specified id or name to complete. Note, if only the name is specified, then the command
     * will work on the first plan item within the case having the specified name. If the plan item is not a task or if no plan item can be found, a
     * CommandFailure will be returned.
     *
     * @param child   The child actor that completes, e.g. a HumanTask or a SubCase
     * @param taskOutput       An optional map with named output parameters for the task. These will be set on the task before the task is reported as complete. This
     *                         means that the parameters will also be bound to the case file, which may cause sentries to activate before the task completes.
     */
    public CompleteTask(ModelActor child, ValueMap taskOutput) {
        super(child.getCurrentUser(), child.getParentActorId());
        this.taskId = child.getId();
        this.taskOutput = taskOutput;
    }

    public CompleteTask(ValueMap json) {
        super(json);
        this.taskId = readField(json, Fields.taskId);
        this.taskOutput = readMap(json, Fields.taskOutput);
    }

    @Override
    public void validate(Case caseInstance) {
        // We bypass invoking super.validate, because super asserts that the user is
        //  part of the case team. However, completion of task from e.g. a SubCase can be done
        //  by a member of _that_ subcase that is NOT a member in _this_ case (parent).
        // In such a scenario, CompleteTask (and FailTask) are successfully done in that sub-case,
        // but not handled in the parent case, leaving state of the CaseTask as Active instead of Completed or Failed.
        // Note: the user is logged as the one completing the task. But the user is NOT added to the CaseTeam.

//        super.validate(caseInstance);

        if (taskId == null || taskId.trim().isEmpty()) {
            throw new InvalidCommandException("Invalid or missing task id");
        }
        PlanItem planItem = getPlanItem(caseInstance);
        if (planItem == null) {
            throw new InvalidCommandException("Invalid or missing task id");
        }

        if (!(planItem instanceof Task)) {
            throw new InvalidCommandException("Invalid or missing task id");
        }

        // Set the task pointer.
        task = (Task) planItem;
    }

    @Override
    public CaseResponse process(Case caseInstance) {
        task.goComplete(taskOutput);
        return new CaseResponse(this);
    }

    private PlanItem getPlanItem(Case caseInstance) {
        return caseInstance.getPlanItemById(taskId);
    }

    @Override
    public String toString() {
        String taskName = task != null ? task.getName() +" with id "+taskId : taskId +" (unknown name)";
        return "CompleteTask "+taskName + " with output\n" + taskOutput;
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.write(generator);
        writeField(generator, Fields.taskId, taskId);
        writeField(generator, Fields.taskOutput, taskOutput);
    }
}
