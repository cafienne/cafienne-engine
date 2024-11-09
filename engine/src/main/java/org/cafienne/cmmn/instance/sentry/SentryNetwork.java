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

import org.cafienne.cmmn.definition.sentry.ExitCriterionDefinition;
import org.cafienne.cmmn.instance.PlanItem;
import org.cafienne.cmmn.instance.casefile.CaseFileItem;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Wrapper class for the collection of all sentries inside the case instance.
 */
public class SentryNetwork {
    private final TransitionCallStack callStack = new TransitionCallStack();
    /**
     * List of sentries active within the case.
     */
    private final Collection<Criterion<?>> criteria = new ArrayList<>();

    /**
     * Connect a new {@link CaseFileItem} to the network
     */
    public void connect(CaseFileItem item) {
        for (Criterion<?> criterion : criteria) {
            criterion.establishPotentialConnection(item);
        }
    }

    /**
     * Disconnect the item from the network (typically when the case file item is lost in migration)
     */
    public void disconnect(CaseFileItem item) {
        criteria.forEach(criterion -> criterion.removeConnection(item));
    }

    /**
     * Connect a new {@link PlanItem} to the network
     */
    public void connect(PlanItem<?> item) {
        // Visit each existing criterion in the network and inform them about the new plan item.
        for (Criterion<?> criterion : criteria) {
            criterion.establishPotentialConnection(item);
        }
    }


    /**
     * Disconnect the item from the network (typically when the plan item is lost in migration)
     */
    public void disconnect(PlanItem<?> item) {
        criteria.forEach(criterion -> criterion.removeConnection(item));
    }

    /**
     * Add a criterion to the network
     */
    void add(Criterion<?> criterion) {
        criteria.add(criterion);
    }

    /**
     * Remove a criterion from the network
     */
    void remove(Criterion<?> criterion) {
        this.criteria.remove(criterion);
    }

    @Override
    public String toString() {
        StringBuilder string = new StringBuilder("SentryNetwork has " + criteria.size()+ " criteria: ");
        criteria.forEach(c -> string.append("\n\t- ").append(c));
        return string + "\n";
    }

    /**
     * Some entry criteria may listen not only to plan items, but also to a specific exit criterion of a plan item.
     * They can retrieve it through this method. Note this method will not create the criterion...
     * @return The related criterion, or null if it is not found.
     */
    Criterion<?> findRelatedExitCriterion(PlanItem<?> item, ExitCriterionDefinition definition) {
       for (Criterion<?> criterion : criteria) {
           if (criterion.getDefinition().equals(definition)) {
                if (criterion.getTarget() == item) {
                        return criterion;
                }
           }
        }
        // Pretty weird, not sure what to do here. Probably we need to make it such that when the
        // exit criterion is created, it will also connect to those entry criteria that relate to it.
        return null;
    }

    public void handleTransition(StandardEvent<?,?> event) {
        callStack.pushEvent(event);
    }
}
