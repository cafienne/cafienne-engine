/* 
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.akka.command.response;

import org.cafienne.akka.actor.command.response.CommandFailure;
import org.cafienne.akka.actor.command.response.ModelResponse;
import org.cafienne.akka.actor.serialization.Fields;
import org.cafienne.akka.actor.serialization.Manifest;
import org.cafienne.cmmn.akka.command.CaseCommand;
import org.cafienne.cmmn.akka.command.GetDiscretionaryItems;
import org.cafienne.cmmn.instance.casefile.ValueMap;

/**
 * If the case instance has handled an {@link CaseCommand}, it will return a CaseResponse to the sender of the command. This can be used to communicate back
 * e.g. a http message code 200 to a web client.
 * A command response usually contains little content. However, there are some exceptions:
 * <ul>
 * <li>The command actually returns a value that cannot be derived from the event stream, e.g. the list of discretionary items, see {@link GetDiscretionaryItems}</li>
 * <li>The command was erroneous and an exception needs to be returned, see {@link CommandFailure}</li>
 * </ul>
 */
@Manifest
public class CaseResponse extends ModelResponse {
    private final String caseInstanceId;

    public CaseResponse(CaseCommand command) {
        super(command);
        this.caseInstanceId = command.getCaseInstanceId();

        // This is the default setting for completion moment: the current last modified of the case instance.
        //  for CommandFailures, or for commands like GetDiscretionaryItems, no events are generated, and the
        //  "case-last-modified" will not change, hence picking the current moment of the case last modified will remain the same.
        // For other commands that actually DO change the case, the case will invoke the method setLastModified() of CaseResponse below.
        setLastModified(command.getActor().getLastModified());
    }

    public CaseResponse(ValueMap json) {
        super(json);
        this.caseInstanceId = readField(json, Fields.caseInstanceId);
    }

    @Override
    public String toString() {
        return "CaseResponse for "+caseInstanceId+": last modified is "+getLastModified();
    }

    public CaseLastModified lastModifiedContent() {
        return new CaseLastModified(caseInstanceId, getLastModified());
    }
}
