package org.cafienne.cmmn.instance;

public enum PlanItemType {
    CasePlan(org.cafienne.cmmn.instance.CasePlan.class),
    Stage(String.valueOf(org.cafienne.cmmn.instance.Stage.class)),
    HumanTask(org.cafienne.cmmn.instance.task.humantask.HumanTask.class),
    CaseTask(org.cafienne.cmmn.instance.task.cmmn.CaseTask.class),
    ProcessTask(org.cafienne.cmmn.instance.task.process.ProcessTask.class),
    UserEvent(org.cafienne.cmmn.instance.UserEvent.class),
    TimerEvent(org.cafienne.cmmn.instance.TimerEvent.class),
    Milestone(org.cafienne.cmmn.instance.Milestone.class);

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

