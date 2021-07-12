package org.cafienne.cmmn.instance.casefile;

import org.cafienne.cmmn.actorapi.event.file.CaseFileEvent;
import org.cafienne.cmmn.instance.sentry.CaseFileItemOnPart;
import org.cafienne.cmmn.instance.sentry.TransitionPublisher;

class CaseFileTransitionPublisher extends TransitionPublisher<CaseFileEvent, CaseFileItem, CaseFileItemOnPart> {
    CaseFileTransitionPublisher(CaseFileItem item) {
        super(item);
    }

    CaseFileTransitionPublisher(CaseFileTransitionPublisher publisher) {
        super(publisher);
    }
}
