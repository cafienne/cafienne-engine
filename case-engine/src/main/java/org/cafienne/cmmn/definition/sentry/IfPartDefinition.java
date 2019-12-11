/* 
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.definition.sentry;

import org.cafienne.cmmn.definition.CMMNElementDefinition;
import org.cafienne.cmmn.definition.ConstraintDefinition;
import org.cafienne.cmmn.definition.Definition;
import org.cafienne.cmmn.instance.sentry.Sentry;
import org.w3c.dom.Element;

public class IfPartDefinition extends ConstraintDefinition {
    public IfPartDefinition(Element element, Definition definition, CMMNElementDefinition parentElement) {
        super(element, definition, parentElement);
    }

    IfPartDefinition(Definition definition, CMMNElementDefinition parentElement) {
        super(definition, parentElement, true); // Default ifPart: evaluates always to true
    }

    public boolean evaluate(Sentry sentry) {
        return getExpressionDefinition().getEvaluator().evaluateIfPart(sentry, this);
    }
}
