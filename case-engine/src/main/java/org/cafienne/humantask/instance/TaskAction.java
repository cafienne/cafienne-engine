/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.humantask.instance;

public enum TaskAction {
    Null("null"),
    Create("create"),
    Claim("claim"),
    Assign("assign"),
    Delegate("delegate"),
    Revoke("revoke"),
    Suspend("suspend"),
    Resume("resume"),
    Complete("complete"),
    Terminate("terminate");

    private final String value;

    TaskAction(String value) {
        this.value = value;
    }

    String getValue() {
        return value;
    }

    public static TaskAction getEnum(String value) {
        for (TaskAction action : values())
            if (action.getValue().equalsIgnoreCase(value)) return action;
        throw new IllegalArgumentException(value + " is not a valid action");
    }
}
