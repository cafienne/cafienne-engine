/* 
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.instance;

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

    private String value;

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
}
