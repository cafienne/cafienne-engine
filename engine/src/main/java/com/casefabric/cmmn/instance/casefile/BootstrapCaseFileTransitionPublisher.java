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

package com.casefabric.cmmn.instance.casefile;

import com.casefabric.cmmn.actorapi.event.file.CaseFileItemTransitioned;

import java.util.ArrayList;
import java.util.List;

public class BootstrapCaseFileTransitionPublisher extends CaseFileTransitionPublisher {
    private final List<CaseFileItemTransitioned> bootstrapEvents = new ArrayList<>();

    BootstrapCaseFileTransitionPublisher(CaseFileItem item) {
        super(item);
        addDebugInfo(() -> "Creating delayed publisher for " + item);
    }

    @Override
    public void addEvent(CaseFileItemTransitioned event) {
        addDebugInfo(() -> "Adding delayed event " + event.getTransition() + " to myself");
        bootstrapEvents.add(event);
        super.updateItemState(event);
    }

    @Override
    public void releaseBootstrapEvents() {
        addDebugInfo(() -> "BootstrapPublisher["+item+"]: releasing " + bootstrapEvents.size() + " events generated from case input parameters");
        bootstrapEvents.forEach(super::informSentryNetwork);
    }
}
