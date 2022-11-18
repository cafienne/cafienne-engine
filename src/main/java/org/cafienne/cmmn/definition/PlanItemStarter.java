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

package org.cafienne.cmmn.definition;

public class PlanItemStarter {

    enum Reason {
        Later("", false),
        NoEntryCriteria("does not have entry criteria"),
        IsCasePlan("always starts immediately"),
        NoOnParts("has an entry criterion with only an if part expression"),
        ImmediateMilestone("depends on a milestone that occurs immediately"),
        ImmediateTimer("depends on a timer that will not be canceled"),
        ImmediateCaseFileCreation("depends on a case file item that may get created immediately through a case input parameter");

        private final String value;
        public final boolean isImmediate;

        Reason(String value, boolean isImmediate) {
            this.isImmediate = isImmediate;
            this.value = value;
        }

        Reason(String value) {
            this(value, true);
        }

        @Override
        public String toString() {
            return value;
        }
    }

    private final Reason reason;
    private final ItemDefinition item;
    private final PlanItemStarter source;

    PlanItemStarter(ItemDefinition item, Reason reason) {
        this(item, reason, null);
    }

    PlanItemStarter(ItemDefinition item, Reason reason, PlanItemStarter source) {
        this.item = item;
        this.reason = reason;
        this.source = source;
    }

    boolean isLater() {
        return !isImmediate();
    }

    boolean isImmediate() {
        if (source == null) {
            return this.reason.isImmediate;
        } else {
            return source.isImmediate();
        }
    }

    private String report(boolean hasParent) {
        String prefix = hasParent ? " and " : "";
        if (source == null) return prefix + item + " " + reason;
        return prefix + item + " " + reason + "\n" + source.report(true);
    }

    @Override
    public String toString() {
        return report(false);
    }

    static PlanItemStarter Later(ItemDefinition item) {
        return new PlanItemStarter(item, Reason.Later);
    }

    static PlanItemStarter hasImmediateMilestone(ItemDefinition item, PlanItemStarter source) {
        return new PlanItemStarter(item, Reason.ImmediateMilestone, source);
    }

    static public PlanItemStarter hasNoEntryCriteria(ItemDefinition item, PlanItemStarter source) {
        return new PlanItemStarter(item, Reason.NoEntryCriteria, source);
    }

    static public PlanItemStarter isCasePlan(ItemDefinition item) {
        return new PlanItemStarter(item, Reason.IsCasePlan);
    }

    static public PlanItemStarter hasNoOnParts(ItemDefinition item) {
        return new PlanItemStarter(item, Reason.NoOnParts);
    }

    static public PlanItemStarter hasImmediateTimer(ItemDefinition item, PlanItemStarter source) {
        return new PlanItemStarter(item, Reason.ImmediateTimer, source);
    }

    static public PlanItemStarter hasImmediateCaseFileCreation(ItemDefinition item) {
        return new PlanItemStarter(item, Reason.ImmediateCaseFileCreation);
    }
}
