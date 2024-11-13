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

package com.casefabric.cmmn.definition;

import com.casefabric.cmmn.definition.extension.workflow.FourEyesDefinition;
import com.casefabric.cmmn.definition.extension.workflow.RendezVousDefinition;
import com.casefabric.cmmn.definition.sentry.EntryCriterionDefinition;
import com.casefabric.cmmn.definition.sentry.ExitCriterionDefinition;
import com.casefabric.cmmn.definition.sentry.ReactivateCriterionDefinition;
import com.casefabric.cmmn.instance.PlanItemType;

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

    default Collection<ReactivateCriterionDefinition> getReactivatingCriteria() {
        return new ArrayList<>();
    }

    Collection<ExitCriterionDefinition> getExitCriteria();

    FourEyesDefinition getFourEyesDefinition();

    RendezVousDefinition getRendezVousDefinition();

    default boolean hasFourEyes() {
        return getFourEyesDefinition() != null && getFourEyesDefinition().hasReferences();
    }

    default boolean hasRendezVous() {
        return getRendezVousDefinition() != null && getRendezVousDefinition().hasReferences();
    }

    default void checkTaskPairingConstraints() {
        if (this.hasRendezVous() && this.hasFourEyes()) {
            Collection<ItemDefinition> counterparts = this.getRendezVousDefinition().getAllReferences();
            Collection<ItemDefinition> opposites = this.getFourEyesDefinition().getAllReferences();
            // Verify that we cannot have "rendez-vous" with items that we also have "4-eyes" with.
            opposites.forEach(item -> {
                if (counterparts.contains(item)) {
                    getModelDefinition().addDefinitionError(getContextDescription() + " has a 4-eyes defined with " + item.getName() +", but also rendez-vous (either directly or indirectly). This is not valid.");
                }
            });
        }
    }

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
        return !getExitCriteria().isEmpty();
    }
}
