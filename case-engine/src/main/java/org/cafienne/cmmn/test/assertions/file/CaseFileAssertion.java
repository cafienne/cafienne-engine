package org.cafienne.cmmn.test.assertions.file;

import org.cafienne.cmmn.actorapi.event.file.CaseFileEvent;
import org.cafienne.cmmn.instance.casefile.Path;
import org.cafienne.cmmn.test.ModelTestCommand;
import org.cafienne.cmmn.test.assertions.ModelTestCommandAssertion;
import org.cafienne.cmmn.test.assertions.PublishedEventsAssertion;
import org.cafienne.cmmn.test.filter.EventFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class CaseFileAssertion extends ModelTestCommandAssertion {
    private final static Logger logger = LoggerFactory.getLogger(CaseFileAssertion.class);
    private final Map<Path, CaseFileItemAssertion> assertions = new HashMap();

    public CaseFileAssertion(ModelTestCommand command) {
        super(command);
        PublishedEventsAssertion<CaseFileEvent> allCaseFileEvents = command.getEventListener().getEvents().filter(CaseFileEvent.class);
        allCaseFileEvents.getEvents().forEach(e -> assertCaseFileItem(e.getPath()).addEvent(e));
//        System.out.println("\n\nWe have "+assertions.size()+" assertions: " + assertions.keySet());
    }

    /**
     * Wait for a CaseFileEvent with the specified path to match the filter
     * @param path
     * @param filter
     * @param optionalDuration
     * @return
     */
    public CaseFileEvent awaitCaseFileEvent(Path path, EventFilter<CaseFileEvent> filter, long... optionalDuration) {
        logger.debug("Waiting for case file event on path "+path);
        return testCommand.getEventListener().waitUntil("CaseFileEvent-"+path, CaseFileEvent.class, event -> {
            boolean pathMatches = path.matches(event.getPath());
            if (pathMatches) {
                logger.debug("Receiving case file event "+event);
            }
            return (! (!pathMatches || !filter.matches(event)));
        }, optionalDuration);
    }

    /**
     * Returns a CaseFileItemAssertion wrapper for the given path
     *
     * @param path item name
     * @return CaseFileItemAssertion
     */
    public CaseFileItemAssertion assertCaseFileItem(Path path) {
        Path existingPath = assertions.keySet().stream().filter(key -> key.matches(path)).findAny().orElse(null);
        if (existingPath == null) existingPath = path;
        CaseFileItemAssertion cfia = assertions.getOrDefault(existingPath, new CaseFileItemAssertion(this, testCommand, path));
        assertions.put(existingPath, cfia);
        return cfia;
    }

    List<CaseFileItemAssertion> getArrayElements(Path path) {
        List<CaseFileItemAssertion> children = new ArrayList();
        assertions.entrySet().stream().filter(entry -> entry.getKey().isArrayElementOf(path)).forEach(entry -> children.add(entry.getValue()));
//        System.out.println("Sorting "+children.size());
        children.sort(Comparator.comparing(CaseFileItemAssertion::getIndexInArray));
        return children;
    }
}
