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

package org.cafienne.engine.cmmn.expression.spel.api.cmmn.constraint;

import org.cafienne.engine.cmmn.definition.sentry.IfPartDefinition;
import org.cafienne.engine.cmmn.instance.sentry.Criterion;

/**
 * Context for evaluation of an if part in a criterion.
 */
public class IfPartAPI extends PlanItemRootAPI<IfPartDefinition> {
    public IfPartAPI(IfPartDefinition ifPartDefinition, Criterion<?> criterion) {
        super(ifPartDefinition, criterion.getTarget());
    }

    @Override
    public String getDescription() {
        return "ifPart in sentry";
    }
}
