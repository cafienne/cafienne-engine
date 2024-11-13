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

package com.casefabric.actormodel.identity;

public enum Origin {
    Tenant("Tenant"),
    Platform("Platform"),
    IDP("IDP"),
    PlatformOwner("PlatformOwner"),
    TimerService("TimerService"),
    Anonymous("Anonymous");

    private final String value;

    Origin(String value) {
        this.value = value;
    }

    public boolean isTenant() {
        return this == Tenant;
    }

    public boolean isPlatform() {
        return this == Platform;
    }

    public boolean isIDP() {
        return this == IDP;
    }

    @Override
    public String toString() {
        return value;
    }

    public static Origin getEnum(String value) {
        if (value == null) return null;
        for (Origin origin : values())
            if (origin.toString().equalsIgnoreCase(value)) return origin;
        return null;
    }
}
