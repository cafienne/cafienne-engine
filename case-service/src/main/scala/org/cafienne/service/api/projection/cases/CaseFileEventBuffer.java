package org.cafienne.service.api.projection.cases;

import org.cafienne.cmmn.akka.event.file.CaseFileEvent;
import org.cafienne.cmmn.akka.event.file.CaseFileItemChildRemoved;
import org.cafienne.cmmn.instance.casefile.Path;

import java.util.ArrayList;
import java.util.List;

class CaseFileEventBuffer {
    private List<CaseFileEvent> events = new ArrayList();
    private List<CaseFileEvent> removedChildren = new ArrayList();

    /**
     * Return the list of events to be handled in the current transaction
     * @return
     */
    public List<CaseFileEvent> events() {
        events.addAll(removedChildren);
        return events;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < events.size(); i++) {
            CaseFileEvent event = events.get(i);
            sb.append(i +":\t" + event.getClass().getSimpleName() + "["+event.path+"]\n");
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
    void addEvent(CaseFileEvent newEvent) {
        // Store RemoveChild events separately
        if (newEvent instanceof CaseFileItemChildRemoved) {
            removedChildren.add(newEvent);
            return;
        }
        // First check to see if any of the existing events overrides our event; if so, just ignore the event.
        for (int i = 0; i < events.size(); i++) {
            CaseFileEvent current = events.get(i);
            Path currentPath = current.path;
            if (currentPath.hasChild(newEvent.path)) {
//                System.out.println("Event[" + i + "] on path " + currentPath + " overrides new event on path " + newPath);
                return;
            }
        }
        // Apparently the new event has a top-level path; start removing all existing events that are under the new event.
        // Reversely go through our array to avoid strange index behavior
        for (int i = events.size() - 1 ; i >= 0 ; i--) {
            CaseFileEvent current = events.get(i);
            Path currentPath = current.path;
            Path newPath = newEvent.path;
            if (newPath.hasChild(currentPath)) {
//                System.out.println("Event[" + i + "] on path " + currentPath + " is overridden with new event on path " + newPath);
                events.remove(i);
            }
        }
//        System.out.println("Buffering case file event " + event);
        events.add(newEvent);
    }
}
