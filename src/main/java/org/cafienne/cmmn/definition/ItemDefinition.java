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

import org.cafienne.cmmn.definition.sentry.EntryCriterionDefinition;
import org.cafienne.cmmn.definition.sentry.ExitCriterionDefinition;
import org.cafienne.cmmn.instance.PlanItemType;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Interface to generalize across PlanItemDefinition and DiscretionaryItemDefinition (and CasePlanDefinition)
 */
public interface ItemDefinition extends DefinitionElement {
    ItemControlDefinition getPlanItemControl();

    PlanItemDefinitionDefinition getPlanItemDefinition();

    default Collection<EntryCriterionDefinition> getEntryCriteria() {
        return new ArrayList<>();
    }

    Collection<ExitCriterionDefinition> getExitCriteria();

    default boolean isDiscretionary() {
        return false;
    }

    default PlanItemType getItemType() {
        return getPlanItemDefinition().getItemType();
    }

    default PlanItemStarter getStarter() {
        return PlanItemStarter.Later(this);
    }

    /**
     * Indication whether exit criteria are defined on this item, or if not, on any of it's parent elements
     * @return
     */
    default boolean hasExits() {
        return getExitCriteria().size() > 0;
    }
}
