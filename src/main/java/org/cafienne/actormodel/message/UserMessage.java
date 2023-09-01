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

package org.cafienne.actormodel.message;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import org.cafienne.actormodel.ModelActor;
import org.cafienne.actormodel.command.BootstrapMessage;
import org.cafienne.actormodel.identity.UserIdentity;
import org.cafienne.infrastructure.serialization.CafienneSerializable;
import org.cafienne.infrastructure.serialization.CafienneSerializer;
import org.cafienne.infrastructure.serialization.Fields;
import org.cafienne.json.JSONParseFailure;
import org.cafienne.json.JSONReader;
import org.cafienne.json.Value;
import org.cafienne.json.ValueMap;

import java.io.IOException;
import java.io.StringWriter;

/**
 * A UserMessage carries user information
 * Typically used in Commands and resulting Events and Responses from those commands.
 */
public interface UserMessage extends CafienneSerializable {
    UserIdentity getUser();

    /**
     * Explicit method to be implemented returning the type of the ModelActor handling this message.
     * This is required for the message routing within the CaseSystem
     *
     * @return
     */
    default Class<?> actorClass() {
        return ModelActor.class;
    }

    default boolean isBootstrapMessage() {
        return false;
    }

    default BootstrapMessage asBootstrapMessage() {
        return (BootstrapMessage) this;
    }

    /**
     * Return a ValueMap serialization of the message
     */
    default ValueMap rawJson() {
        JsonFactory factory = new JsonFactory();
        StringWriter sw = new StringWriter();
        try {
            JsonGenerator generator = factory.createGenerator(sw);
            generator.setPrettyPrinter(new DefaultPrettyPrinter());
            writeThisObject(generator);
            generator.close();
            Value<?> json = JSONReader.parse(sw.toString());
            return new ValueMap(Fields.type, CafienneSerializer.getManifestString(this), Fields.content, json);
        } catch (IOException | JSONParseFailure e) {
            return new ValueMap("message", "Could not make JSON out of " + getClass().getName(), "exception", Value.convertThrowable(e));
        }
    }
}
