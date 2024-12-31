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

package org.cafienne.json;

import com.fasterxml.jackson.core.*;

import java.io.IOException;
import java.io.InputStream;

/**
 * Too simple JSON parser, that generates Value<?> objects.
 * Probably needs to be extended to support all the JSON databind features (i.e.,
 * we ought to make it such that it can be replaced with that one)
 *
 */
public class JSONReader {

    private static JsonFactory getJSONFactory() {
        JsonFactory factory = new JsonFactory();
        factory.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
        factory.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
        factory.configure(JsonParser.Feature.ALLOW_COMMENTS, true);
        factory.configure(JsonParser.Feature.ALLOW_YAML_COMMENTS, true);
        return factory;
    }

    /**
     * Parse the specified string into a {@link Value} object
     * @param jsonString
     * @return
     * @throws JsonParseException
     * @throws IOException
     */
    public static <T extends Value<?>> T parse(String jsonString) throws IOException, JSONParseFailure {
        JsonParser jp = getJSONFactory().createParser(jsonString);
        return (T) read(jp, null);
    }

    public static <T extends Value<?>> T parse(InputStream jsonStream) throws IOException, JSONParseFailure {
        JsonParser jp = getJSONFactory().createParser(jsonStream);
        return (T) read(jp, null);
    }

    /**
     * Parse the bytes into a {@link Value} object.
     * @param bytes
     * @return
     * @throws IOException
     */
    public static <T extends Value<?>> T parse(byte[] bytes) throws IOException, JSONParseFailure {
        JsonParser jp = getJSONFactory().createParser(bytes);
        return (T) read(jp, null);
    }

    public static Value<?> read(JsonParser jp, Value<?> currentParent) throws IOException, JSONParseFailure {

        if (jp.getCurrentToken() == null)
            jp.nextToken();

        // Iterate over object fields:
        // JsonToken token = jp.getCurrentToken();
        int tokenId = jp.currentTokenId();

        Value<?> value = null;

        switch (tokenId) {
        case JsonTokenId.ID_NULL: {
            value = Value.NULL;
            break;
        }
        case JsonTokenId.ID_NUMBER_INT: {
            value = new LongValue(jp.getLongValue());
            break;
        }
        case JsonTokenId.ID_NUMBER_FLOAT: {
            value = new DoubleValue(jp.getDoubleValue());
            break;
        }
        case JsonTokenId.ID_STRING: {
            value = new StringValue(jp.getText());
            break;
        }
        case JsonTokenId.ID_TRUE: {
            value = new BooleanValue(true);
            break;
        }
        case JsonTokenId.ID_FALSE: {
            value = new BooleanValue(false);
            break;
        }
        case JsonTokenId.ID_START_ARRAY: {
            ValueList array = new ValueList();
            jp.nextToken();
            while (jp.getCurrentToken() != JsonToken.END_ARRAY) {
                array.add(read(jp, currentParent));
                jp.nextToken();
            }
            value = array;
            break;
        }
        case JsonTokenId.ID_START_OBJECT: {
            jp.nextToken();
            value = read(jp, currentParent);
            break;
        }
        case JsonTokenId.ID_FIELD_NAME:
        case JsonTokenId.ID_END_OBJECT: {
            Value<?> grandParent = currentParent;
            ValueMap object = new ValueMap();
            currentParent = object;
            while (jp.getCurrentToken().id() != JsonTokenId.ID_END_OBJECT) {
                String fieldName = jp.getCurrentName();
                jp.nextToken();
                Value<?> fieldValue = read(jp, currentParent);
                object.put(fieldName, fieldValue);
                jp.nextToken();
            }
            currentParent = grandParent;
            value = object;
            break;
        }
        case JsonTokenId.ID_EMBEDDED_OBJECT: {
            value = new BinaryValue(jp.getBinaryValue());
            break;
        }
        case JsonTokenId.ID_NO_TOKEN:
        case JsonTokenId.ID_NOT_AVAILABLE: {
            throw new JSONParseFailure("JsonTokenId: NO_TOKEN or NOT_AVAILABLE; cannot parse into a valid JSON object");
        }
        }

        return value;
    }
}
