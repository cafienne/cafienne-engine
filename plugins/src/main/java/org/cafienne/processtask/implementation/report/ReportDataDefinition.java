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

package org.cafienne.processtask.implementation.report;

import org.cafienne.cmmn.definition.CMMNElementDefinition;
import org.cafienne.cmmn.definition.ModelDefinition;
import org.cafienne.json.Value;
import org.w3c.dom.Element;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class ReportDataDefinition extends CMMNElementDefinition {
    static InputStream EMPTY_STREAM = new ByteArrayInputStream("{}".getBytes(StandardCharsets.UTF_8));
    private final String name;

    public ReportDataDefinition(Element element, ModelDefinition modelDefinition, CMMNElementDefinition parentElement) {
        super(element, modelDefinition, parentElement);
        this.name = parseAttribute("name", false);
    }

    /**
     * Sets the report data
     */
    public InputStream createDataStream(PDFReport report) {
        // ReportData is taken from an input parameter with the name 'reportData', or, alternatively,
        //  from an <reportData name="reference-to-data-parameter"> tag inside the definition.

        if (this.name.isEmpty()) {
            // Just return an empty data stream; apparently data is not needed for this report.
            return EMPTY_STREAM;
        }

        if (!report.getInputParameters().has(name)) {
            throw new MissingParameterException("Report data '" + name + "' cannot be found in the task input parameters");
        }

        // Take the parameter value, flatten it to string and then return it as a stream. Can probably be done
        //  more efficiently...
        Value<?> jsonData = report.getInputParameters().get(name);
        return new ByteArrayInputStream(jsonData.toString().getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public boolean equalsWith(Object object) {
        return notYetImplemented();
    }
}
