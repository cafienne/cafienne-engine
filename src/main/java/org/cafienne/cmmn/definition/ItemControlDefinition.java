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

import org.w3c.dom.Element;

public class ItemControlDefinition extends CMMNElementDefinition {
    private final ConstraintDefinition requiredRule;
    private final ConstraintDefinition repetitionRule;
    private final ConstraintDefinition manualActivationRule;

    ItemControlDefinition(ModelDefinition definition, CMMNElementDefinition parentElement) {
        // TODO: it is better to parse a default XML element?
        //  - but that has more serious impact, since the null element inside the ExpressionDefinition
        //  and the defaultValue is used to decide to create a DefaultValueEvaluator, and
        // ... that is used to determine inside the plan item whether a repetitionRule is default or not
        // ... and that info is used ... in quite a few places (4 or 5)
        this(null, definition, parentElement);
    }

    public ItemControlDefinition(Element element, ModelDefinition modelDefinition, CMMNElementDefinition parentElement) {
        super(element, modelDefinition, parentElement);
        repetitionRule = parseRule(element, "repetitionRule", false);
        requiredRule = parseRule(element, "requiredRule", false);
        manualActivationRule = parseRule(element, "manualActivationRule", false);
    }

    private ConstraintDefinition parseRule(Element element, String type, boolean defaultValue) {
        ConstraintDefinition rule = null;
        if (element != null) {
            rule = parse(type, ConstraintDefinition.class, false);
        }
        if (rule == null) { // Create a default rule
            rule = new ConstraintDefinition(getModelDefinition(), this, type, defaultValue);
        }
        return rule;
    }

    public ConstraintDefinition getRequiredRule() {
        return requiredRule;
    }

    public ConstraintDefinition getRepetitionRule() {
        return repetitionRule;
    }

    public ConstraintDefinition getManualActivationRule() {
        return manualActivationRule;
    }

    @Override
    public boolean equalsWith(Object object) {
        return equalsWith(object, this::sameItemControl);
    }

    public boolean sameItemControl(ItemControlDefinition other) {
        boolean sameSuper = super.sameClass(other); // Name and id are not relevant
        boolean sameRequiredRule = same(requiredRule, other.requiredRule);
        boolean sameRepetitionRule = same(repetitionRule, other.repetitionRule);
        boolean sameManualActivationRule = same(manualActivationRule, other.manualActivationRule);
        return sameSuper && sameRequiredRule && sameRepetitionRule && sameManualActivationRule;
    }
}
