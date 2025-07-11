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

package org.cafienne.engine.cmmn.test.assertions.file;

import org.cafienne.engine.cmmn.actorapi.event.file.CaseFileItemTransitioned;
import org.cafienne.engine.cmmn.instance.Path;
import org.cafienne.engine.cmmn.test.CaseTestCommand;
import org.cafienne.engine.cmmn.test.assertions.ModelTestCommandAssertion;
import org.cafienne.engine.cmmn.test.assertions.PublishedEventsAssertion;
import org.cafienne.engine.cmmn.test.filter.EventFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class CaseFileAssertion extends ModelTestCommandAssertion {
    private final static Logger logger = LoggerFactory.getLogger(CaseFileAssertion.class);
    private final Map<Path, CaseFileItemAssertion> assertions = new HashMap<>();

    public CaseFileAssertion(CaseTestCommand command) {
        super(command);
        PublishedEventsAssertion<CaseFileItemTransitioned> allCaseFileItemTransitioneds = command.getEventListener().getEvents().filter(CaseFileItemTransitioned.class);
        allCaseFileItemTransitioneds.filter(CaseFileItemTransitioned.class).getEvents().forEach(e -> assertCaseFileItem(e.getPath()).addEvent(e));
//        System.out.println("\n\nWe have "+assertions.size()+" assertions: " + assertions.keySet());
    }

    /**
     * Wait for a CaseFileItemTransitioned with the specified path to match the filter
     * @param path
     * @param filter
     * @param optionalDuration
     * @return
     */
    public CaseFileItemTransitioned awaitCaseFileEvent(Path path, EventFilter<CaseFileItemTransitioned> filter, long... optionalDuration) {
        logger.debug("Waiting for case file event on path "+path);
        return testCommand.getEventListener().waitUntil("CaseFileItemTransitioned-"+path, CaseFileItemTransitioned.class, event -> {
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
        List<CaseFileItemAssertion> children = new ArrayList<>();
        assertions.entrySet().stream().filter(entry -> entry.getKey().isArrayElementOf(path)).forEach(entry -> children.add(entry.getValue()));
//        System.out.println("Sorting "+children.size());
        children.sort(Comparator.comparing(CaseFileItemAssertion::getIndexInArray));
        return children;
    }
}
