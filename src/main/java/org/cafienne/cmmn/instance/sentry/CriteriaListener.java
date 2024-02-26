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

package org.cafienne.cmmn.instance.sentry;

import org.cafienne.cmmn.definition.DefinitionElement;
import org.cafienne.cmmn.definition.ItemDefinition;
import org.cafienne.cmmn.definition.sentry.CriterionDefinition;
import org.cafienne.cmmn.instance.CMMNElement;
import org.cafienne.cmmn.instance.PlanItem;
import org.cafienne.cmmn.instance.PlanItemEntry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Locale;

public abstract class CriteriaListener<T extends CriterionDefinition, C extends Criterion<T>> extends CMMNElement<ItemDefinition> {
    protected final PlanItem<?> item;
    protected final Collection<C> criteria = new ArrayList<>();
    protected final Collection<T> definitions;
    private final String logDescription;

    protected CriteriaListener(PlanItem<?> item, Collection<T> definitions) {
        super(item, item.getItemDefinition());
        this.item = item;
        this.definitions = definitions;
        this.logDescription = getClass().getSimpleName().substring(8).toLowerCase(Locale.ROOT);
    }

    /**
     * Start listening to the sentry network
     */
    public void startListening() {
        this.definitions.forEach(this::addCriterion);
        if (!criteria.isEmpty()) {
            item.getCaseInstance().addDebugInfo(() -> "Connected " + item + " to " + criteria.size() + " " + logDescription + " criteria");
        }
    }

    private void addCriterion(T definition) {
        C criterion = createCriterion(definition);
        criteria.add(criterion);
    }

    protected abstract C createCriterion(T definition);

    /**
     * Stop listening to the sentry network, typically when the criterion is satisfied.
     */
    public void stopListening() {
        if (!criteria.isEmpty()) {
            addDebugInfo(() -> "Disconnecting " + item + " from " + criteria.size() + " " + logDescription + " criteria");
        }
        new ArrayList<>(criteria).forEach(this::release);
    }

    /**
     * Removes the criterion from our collection and tells the criterion to disconnect from the network.
     *
     * @param criterion
     */
    private void release(C criterion) {
        criteria.remove(criterion);
        criterion.release();
    }

    public abstract void satisfy(Criterion<?> criterion);

    protected abstract void migrateCriteria(ItemDefinition newItemDefinition, boolean skipLogic);

    protected void migrateCriteria(Collection<T> newDefinitions, boolean skipLogic) {
        addDebugInfo(() -> {
            if (criteria.isEmpty() && newDefinitions.isEmpty()) {
                return "";
            } else {
                String criteriaType = this instanceof PlanItemEntry ? "entry" : "exit";
                return "Migrating " + criteriaType + " criteria of " + item;
            }
        });
        Collection<C> existingCriteria = new ArrayList<>(criteria);

        existingCriteria.forEach(criterion -> migrateCriterion(criterion, newDefinitions, skipLogic));
        newDefinitions.stream().filter(this::hasCriterion).forEach(this::addCriterion);
    }

    private boolean hasCriterion(T definition) {
        return this.criteria.stream().noneMatch(c -> c.getDefinition() == definition);
    }

    private void migrateCriterion(C criterion, Collection<T> newDefinitions, boolean skipLogic) {
        T oldDefinition = criterion.getDefinition();
        T newDefinition = DefinitionElement.findDefinition(oldDefinition, newDefinitions);
        if (newDefinition != null) {
            criterion.migrateDefinition(newDefinition, skipLogic);
        } else {
            // Not sure what to do here. Remove the criterion?
            // Search for a 'nearby' alternative?
            addDebugInfo(() -> "Dropping criterion " + criterion);
            this.release(criterion);
        }
    }
}
