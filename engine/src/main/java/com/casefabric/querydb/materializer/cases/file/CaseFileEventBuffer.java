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

package com.casefabric.querydb.materializer.cases.file;

import com.casefabric.cmmn.actorapi.event.file.CaseFileItemChildRemoved;
import com.casefabric.cmmn.actorapi.event.file.CaseFileItemTransitioned;
import com.casefabric.cmmn.instance.Path;
import com.casefabric.json.ValueMap;

import java.util.ArrayList;
import java.util.List;

class CaseFileEventBuffer {
    private final List<CaseFileItemTransitioned> events = new ArrayList<>();

    public void update(ValueMap caseFileInProgress) {
        events.forEach(event -> CaseFileMerger.merge(event, caseFileInProgress));
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < events.size(); i++) {
            CaseFileItemTransitioned event = events.get(i);
            sb.append(i +":\t" + event.getClass().getSimpleName() + "["+event.getPath()+"]\n");
        }
        return sb.toString();
    }

    /**
     * Selectively add an event to be handled; we only need top-path level events,
     * e.g. replace event on /RootItem and on /RootItem/Child1, /RootItem/Child2 have full required
     * info inside the event for /RootItem.
     * Only special case is for RemoveChild, as that has parent and child path in itself.
     * @param newEvent
     */
    void addEvent(CaseFileItemTransitioned newEvent) {
        Path newPath = newEvent.getPath();

        // New path is different for RemoveChild events
        if (newEvent instanceof CaseFileItemChildRemoved) {
            newPath = ((CaseFileItemChildRemoved) newEvent).getChildPath();
        }

        // First check to see if any of the existing events overrides our event; if so, just ignore the event.
        for (int i = 0; i < events.size(); i++) {
            CaseFileItemTransitioned current = events.get(i);
            Path currentPath = current.getPath();
            if (currentPath.hasChild(newPath)) {
//                System.out.println("Event[" + i + "] on path " + currentPath + " overrides new event on path " + newPath);
                return;
            }
        }
        // Apparently the new event has a top-level path; start removing all existing events that are under the new event.
        // Reversely go through our array to avoid strange index behavior
        for (int i = events.size() - 1 ; i >= 0 ; i--) {
            CaseFileItemTransitioned current = events.get(i);
            Path currentPath = current.getPath();
            if (newPath.hasChild(currentPath)) {
//                System.out.println("Event[" + i + "] on path " + currentPath + " is overridden with new event on path " + newPath);
                events.remove(i);
            }
        }
//        System.out.println("Buffering case file event " + event);
        events.add(newEvent);
    }
}
