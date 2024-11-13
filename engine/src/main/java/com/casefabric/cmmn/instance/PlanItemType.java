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

package com.casefabric.cmmn.instance;

public enum PlanItemType {
    CasePlan(com.casefabric.cmmn.instance.CasePlan.class),
    Stage(String.valueOf(com.casefabric.cmmn.instance.Stage.class)),
    HumanTask(com.casefabric.cmmn.instance.task.humantask.HumanTask.class),
    CaseTask(com.casefabric.cmmn.instance.task.cmmn.CaseTask.class),
    ProcessTask(com.casefabric.cmmn.instance.task.process.ProcessTask.class),
    UserEvent(com.casefabric.cmmn.instance.UserEvent.class),
    TimerEvent(com.casefabric.cmmn.instance.TimerEvent.class),
    Milestone(com.casefabric.cmmn.instance.Milestone.class);

    PlanItemType(String value) {
    }

    PlanItemType(Class<? extends PlanItem<?>> clazz) {
        this(clazz.getSimpleName());
    }

    public boolean isCasePlan() {
        return this == CasePlan;
    }

    public boolean isStage() {
        return this == Stage;
    }

    public boolean isTask() {
        return isHumanTask() || isCaseTask() || isProcessTask();
    }

    public boolean isHumanTask() {
        return this == HumanTask;
    }

    public boolean isCaseTask() {
        return this == CaseTask;
    }

    public boolean isProcessTask() {
        return this == ProcessTask;
    }

    public boolean isUserEvent() {
        return this == UserEvent;
    }

    public boolean isTimerEvent() {
        return this == TimerEvent;
    }

    public boolean isMilestone() {
        return this == Milestone;
    }
}

