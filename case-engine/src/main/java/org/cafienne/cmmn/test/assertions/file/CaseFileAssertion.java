package org.cafienne.cmmn.test.assertions.file;

import org.cafienne.cmmn.akka.event.file.CaseFileEvent;
import org.cafienne.cmmn.test.ModelTestCommand;
import org.cafienne.cmmn.test.assertions.ModelTestCommandAssertion;
import org.cafienne.cmmn.test.assertions.PublishedEventsAssertion;
import org.cafienne.cmmn.test.filter.EventFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class CaseFileAssertion extends ModelTestCommandAssertion {
    private final static Logger logger = LoggerFactory.getLogger(CaseFileAssertion.class);
    private final Map<String, CaseFileItemAssertion> assertions = new HashMap();

    public CaseFileAssertion(ModelTestCommand command) {
        super(command);
        PublishedEventsAssertion<CaseFileEvent> allCaseFileEvents = command.getEventListener().getEvents().filter(CaseFileEvent.class);
        allCaseFileEvents.getEvents().forEach(e -> {
            CaseFileItemAssertion cfia = assertions.getOrDefault(e.getPath(), new CaseFileItemAssertion(this, command, e.getPath()));
            assertions.put(e.getPath(), cfia);
            cfia.addEvent(e);
        });
//        System.out.println("\n\nWe have "+assertions.size()+" assertions: " + assertions.keySet());
    }

    /**
     * Wait for a CaseFileEvent with the specified path to match the filter
     * @param path
     * @param filter
     * @param optionalDuration
     * @return
     */
    public CaseFileEvent awaitCaseFileEvent(String path, EventFilter<CaseFileEvent> filter, long... optionalDuration) {
        logger.debug("Waiting for case file event on path "+path);
        return testCommand.getEventListener().waitUntil("CaseFileEvent-"+path, CaseFileEvent.class, event -> {
            boolean pathMatches = path.equals(event.getPath()) || path.matches(cleanPath(event.getPath()));
            if (pathMatches) {
                logger.debug("Receiving case file event "+event);
            }
            return (! (!pathMatches || !filter.matches(event)));
        }, optionalDuration);
    }

    /**
     * Returns a CaseFileItemAssertion wrapper for the given path
     *
     * @param fileItemPath item name
     * @return CaseFileItemAssertion
     */
    public CaseFileItemAssertion assertCaseFileItem(String fileItemPath) {
        CaseFileItemAssertion cfia = assertions.get(fileItemPath);
        if (cfia == null) {
            // Return a dummy, none existing CFIA.
            cfia = new CaseFileItemAssertion(this, testCommand, fileItemPath);
            assertions.put(fileItemPath, cfia);
        }
        return cfia;
    }

    List<CaseFileItemAssertion> getArrayElements(String path) {
        List<CaseFileItemAssertion> children = new ArrayList();
        assertions.forEach((key, value) -> {
            if (key.startsWith(path)) {
                String restOfKey = key.substring(path.length());
//                System.out.println("Potential child in " + path + ": second part is "+restOfKey);
                if (restOfKey.startsWith("[") && restOfKey.indexOf(']') == restOfKey.length() - 1) {
                    children.add(value);
//                } else {
//                    System.out.println("NO match after all...");
                }
            }
        });
//        System.out.println("Sorting "+children.size());
        children.sort(Comparator.comparing(CaseFileItemAssertion::getIndexInArray));
        return children;
    }

    private String cleanPath(String path) {
        int openBracket = path.indexOf('[');
        int closeBracket = path.indexOf(']');
        if (openBracket > 0 && closeBracket > openBracket) {
            String newPath = path.substring(0, openBracket) + path.substring(closeBracket + 1);
//            System.out.println("Further cleansing path from "+path+" with "+newPath);
            return cleanPath(newPath);
        } else {
//            System.out.println("No more cleansing of path. Returning "+path);
            return path;
        }
    }
}
