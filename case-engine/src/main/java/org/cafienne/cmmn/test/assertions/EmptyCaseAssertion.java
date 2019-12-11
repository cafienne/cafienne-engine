package org.cafienne.cmmn.test.assertions;

import org.cafienne.cmmn.akka.event.CaseDefinitionApplied;
import org.cafienne.cmmn.test.CaseTestCommand;

public class EmptyCaseAssertion extends CaseAssertion {
    public EmptyCaseAssertion(CaseTestCommand testCommand) {
        super(testCommand);
        if (! new PublishedEventsAssertion(testCommand.getEventListener().getEvents()).filter(testCommand.getCaseInstanceId()).filter(CaseDefinitionApplied.class).getEvents().isEmpty()) {
            throw new AssertionError("Case has a definition, but it is not expected to have one");
        }
    }

}
