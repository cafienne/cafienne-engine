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

package org.cafienne.engine.cmmn.instance;

public enum Transition {
    Close("close"),
    Complete("complete"),
    Create("create"),
    Disable("disable"),
    Enable("enable"),
    Exit("exit"),
    Fault("fault"),
    ManualStart("manualStart"),
    None(""), // Instead of NullPointer
    Occur("occur"),
    ParentResume("parentResume"),
    ParentSuspend("parentSuspend"),
    ParentTerminate("parentTerminate"),
    Reactivate("reactivate"),
    Reenable("reenable"),
    Resume("resume"),
    Start("start"),
    Suspend("suspend"),
    Terminate("terminate");

    private final String value;

    Transition(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static Transition getEnum(String value) {
        if (value == null) return null;
        for (Transition transition : values())
            if (transition.getValue().equalsIgnoreCase(value)) return transition;
        return null;
    }

    public boolean isClose() {
        return this == Close;
    }

    public boolean isComplete() {
        return this == Complete;
    }

    public boolean isCreate() {
        return this == Create;
    }

    public boolean isDisable() {
        return this == Disable;
    }

    public boolean isEnable() {
        return this == Enable;
    }

    public boolean isExit() {
        return this == Exit;
    }

    public boolean isFault() {
        return this == Fault;
    }

    public boolean isManualStart() {
        return this == ManualStart;
    }

    public boolean isNone() {
        return this == None;
    }

    public boolean isOccur() {
        return this == Occur;
    }

    public boolean isParentResume() {
        return this == ParentResume;
    }

    public boolean isParentSuspend() {
        return this == ParentSuspend;
    }

    public boolean isParentTerminate() {
        return this == ParentTerminate;
    }

    public boolean isReactivate() {
        return this == Reactivate;
    }

    public boolean isReenable() {
        return this == Reenable;
    }

    public boolean isResume() {
        return this == Resume;
    }

    public boolean isStart() {
        return this == Start;
    }

    public boolean isSuspend() {
        return this == Suspend;
    }

    public boolean isTerminate() {
        return this == Terminate;
    }
}
