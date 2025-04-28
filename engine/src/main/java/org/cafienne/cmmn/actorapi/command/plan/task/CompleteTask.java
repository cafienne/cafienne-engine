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

package org.cafienne.cmmn.actorapi.command.plan.task;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.actormodel.ModelActor;
import org.cafienne.actormodel.exception.InvalidCommandException;
import org.cafienne.cmmn.actorapi.command.CaseCommand;
import org.cafienne.cmmn.actorapi.command.plan.MakePlanItemTransition;
import org.cafienne.cmmn.instance.Case;
import org.cafienne.cmmn.instance.PlanItem;
import org.cafienne.cmmn.instance.Task;
import org.cafienne.cmmn.instance.Transition;
import org.cafienne.infrastructure.serialization.Fields;
import org.cafienne.infrastructure.serialization.Manifest;
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
        super(child.getCurrentUser().asCaseUserIdentity(), child.getParentActorId(), child.getRootActorId());
        this.taskId = child.getId();
        this.taskOutput = taskOutput;
    }

    public CompleteTask(ValueMap json) {
        super(json);
        this.taskId = json.readString(Fields.taskId);
        this.taskOutput = json.readMap(Fields.taskOutput);
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
        PlanItem<?> planItem = getPlanItem(caseInstance);
        if (planItem == null) {
            throw new InvalidCommandException("Invalid or missing task id");
        }

        if (!(planItem instanceof Task)) {
            throw new InvalidCommandException("Invalid or missing task id");
        }

        // Set the task pointer.
        task = (Task<?>) planItem;
    }

    @Override
    public void processCaseCommand(Case caseInstance) {
        task.goComplete(taskOutput);
    }

    private PlanItem<?> getPlanItem(Case caseInstance) {
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
