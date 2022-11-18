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

package org.cafienne.cmmn.instance;

/**
 * Enum describing possible states plan items and case file items
 * Note, case file item can only be in Null, Available and Discarded, whereas plan item cannot be in Discarded, as per the spec.
 * Nevertheless merged here for convenience.
 */
public enum State {
    Null,
    Active,
    Available,
    Closed,
    Completed,
    Disabled,
    Discarded, // Special case for case file item
    Enabled,
    Failed,
    Suspended,
    Terminated;

    /**
     * As per the spec
     *
     * @return
     */
    public boolean isSemiTerminal() {
        return this == Closed || this == Completed || this == Disabled || this == Failed || this == Terminated;
    }

    public boolean isNull() {
        return this == Null;
    }

    public boolean isActive() {
        return this == Active;
    }

    public boolean isAvailable() {
        return this == Available;
    }

    public boolean isClosed() {
        return this == Closed;
    }

    public boolean isCompleted() {
        return this == Completed;
    }

    public boolean isDisabled() {
        return this == Disabled;
    }

    public boolean isDiscarded() {
        return this == Discarded;
    }

    public boolean isEnabled() {
        return this == Enabled;
    }

    public boolean isFailed() {
        return this == Failed;
    }

    public boolean isSuspended() {
        return this == Suspended;
    }

    public boolean isTerminated() {
        return this == Terminated;
    }

    /**
     * Returns true if the state is beyond Null.
     *
     * @return
     */
    public boolean isCreated() {
        return this != Null;
    }

    /**
     * Returns true if the task is beyond creation (i.e., state is not null and not available)
     *
     * @return
     */
    public boolean isInitiated() {
        return this != Null && this != Available;
    }

    /**
     * Returns true if the lifecycle of the task is done (i.e., state is Completed or state is Terminated)
     *
     * @return
     */
    public boolean isDone() {
        return this == Completed || this == Terminated;
    }

    /**
     * Returns true if the lifecycle of the task is initiated and not yet done.
     */
    public boolean isAlive() {
        return this == Active || this == Suspended || this == Failed || this == Enabled || this == Disabled;
    }
}
