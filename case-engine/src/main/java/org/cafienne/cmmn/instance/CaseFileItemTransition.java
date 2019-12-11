/* 
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.instance;

public enum CaseFileItemTransition {
    AddChild("addChild"),
    AddReference("addReference"),
    Create("create"),
    Delete("delete"),
    RemoveChild("removeChild"),
    RemoveReference("removeReference"),
    Replace("replace"),
    Update("update");

    private String value;

    CaseFileItemTransition(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static CaseFileItemTransition getEnum(String value) {
        for (CaseFileItemTransition transition : values())
            if (transition.getValue().equalsIgnoreCase(value)) return transition;
        throw new IllegalArgumentException(value + " is not a valid CaseFileItemTransition");
    }
}