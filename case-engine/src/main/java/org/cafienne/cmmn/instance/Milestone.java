/* 
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.instance;

import org.cafienne.cmmn.definition.ItemDefinition;
import org.cafienne.cmmn.definition.MilestoneDefinition;

public class Milestone extends PlanItem<MilestoneDefinition> {
    public Milestone(String id, int index, ItemDefinition itemDefinition, MilestoneDefinition definition, Stage stage) {
        super(id, index, itemDefinition, definition, stage, StateMachine.EventMilestone);
    }
}
