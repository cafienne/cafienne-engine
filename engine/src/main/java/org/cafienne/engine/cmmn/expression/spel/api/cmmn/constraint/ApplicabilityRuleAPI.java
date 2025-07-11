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

import org.cafienne.engine.cmmn.definition.ApplicabilityRuleDefinition;
import org.cafienne.engine.cmmn.definition.DiscretionaryItemDefinition;
import org.cafienne.engine.cmmn.instance.PlanItem;

/**
 * Applicability rules are executed on discretionary items related to a Stage or HumanTask.
 * This context provides the additional information with the properties <code>planItem</code> and <code>discretionaryItem</code>.
 */
public class ApplicabilityRuleAPI extends PlanItemRootAPI<ApplicabilityRuleDefinition> {
    private final ApplicabilityRuleDefinition ruleDefinition;
    private final DiscretionaryItemDefinition itemDefinition;

    public ApplicabilityRuleAPI(PlanItem<?> planItem, DiscretionaryItemDefinition itemDefinition, ApplicabilityRuleDefinition ruleDefinition) {
        super(ruleDefinition, planItem);
        this.ruleDefinition = ruleDefinition;
        this.itemDefinition = itemDefinition;
        addPropertyReader("discretionaryItem", () -> itemDefinition);
    }

    @Override
    public String getDescription() {
        return "applicability rule '" + ruleDefinition.getName() + "' for discretionary item " + itemDefinition;
    }
}
