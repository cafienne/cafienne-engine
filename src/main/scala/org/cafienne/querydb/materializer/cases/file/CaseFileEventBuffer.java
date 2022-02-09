package org.cafienne.querydb.materializer.cases.file;

import org.cafienne.cmmn.actorapi.event.file.CaseFileItemChildRemoved;
import org.cafienne.cmmn.actorapi.event.file.CaseFileItemTransitioned;
import org.cafienne.cmmn.instance.casefile.Path;

import java.util.ArrayList;
import java.util.List;

class CaseFileEventBuffer {
    private List<CaseFileItemTransitioned> events = new ArrayList<>();
    private List<CaseFileItemTransitioned> removedChildren = new ArrayList<>();

    /**
     * Return the list of events to be handled in the current transaction
     * @return
     */
    public List<CaseFileItemTransitioned> events() {
        events.addAll(removedChildren);
        return events;
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
        // Store RemoveChild events separately
        if (newEvent instanceof CaseFileItemChildRemoved) {
            removedChildren.add(newEvent);
            return;
        }
        // First check to see if any of the existing events overrides our event; if so, just ignore the event.
        for (int i = 0; i < events.size(); i++) {
            CaseFileItemTransitioned current = events.get(i);
            Path currentPath = current.getPath();
            if (currentPath.hasChild(newEvent.getPath())) {
//                System.out.println("Event[" + i + "] on path " + currentPath + " overrides new event on path " + newPath);
                return;
            }
        }
        // Apparently the new event has a top-level path; start removing all existing events that are under the new event.
        // Reversely go through our array to avoid strange index behavior
        for (int i = events.size() - 1 ; i >= 0 ; i--) {
            CaseFileItemTransitioned current = events.get(i);
            Path currentPath = current.getPath();
            Path newPath = newEvent.getPath();
            if (newPath.hasChild(currentPath)) {
//                System.out.println("Event[" + i + "] on path " + currentPath + " is overridden with new event on path " + newPath);
                events.remove(i);
            }
        }
//        System.out.println("Buffering case file event " + event);
        events.add(newEvent);
    }
}
