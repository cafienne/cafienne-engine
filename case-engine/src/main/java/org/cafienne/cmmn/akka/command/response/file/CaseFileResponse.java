/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.akka.command.response.file;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.akka.actor.serialization.Fields;
import org.cafienne.akka.actor.serialization.Manifest;
import org.cafienne.akka.actor.serialization.json.Value;
import org.cafienne.akka.actor.serialization.json.ValueList;
import org.cafienne.akka.actor.serialization.json.ValueMap;
import org.cafienne.cmmn.akka.command.casefile.CaseFileCommand;
import org.cafienne.cmmn.akka.command.response.CaseResponse;

import java.io.IOException;

/**
 *
 */
@Manifest
public class CaseFileResponse extends CaseResponse {
    protected final ValueList information;

    public CaseFileResponse(CaseFileCommand command) {
        super(command);
        information = new ValueList();
    }

    public CaseFileResponse(ValueMap json) {
        super(json);
        information = readArray(json, Fields.information);
    }

    @Override
    public String toString() {
        return "CaseFileResponse for " + getActorId() + ": last modified is " + getLastModified();
    }

    @Override
    public Value getResponse() {
        return information;
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.write(generator);
        writeField(generator, Fields.information, information);
    }
}
