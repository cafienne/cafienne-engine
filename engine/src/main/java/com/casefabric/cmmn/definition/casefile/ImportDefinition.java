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

package org.cafienne.cmmn.definition.casefile;

import org.cafienne.cmmn.definition.DefinitionsDocument;
import org.cafienne.cmmn.definition.ModelDefinition;
import org.w3c.dom.Element;

/**
 * Implementation of CMMN spec 5.1.3
 */
public class ImportDefinition extends ModelDefinition {

    private final String importType;
    private final String location;
    private final String namespace;

    public ImportDefinition(Element definitionElement, DefinitionsDocument document) {
        super(definitionElement, document);

        this.importType = parseAttribute("importType", false, "");
        this.location = parseAttribute("location", false, "");
        this.namespace = parseAttribute("namespace", false, "");
    }

    public String getImportType() {
        return importType;
    }

    public String getLocation() {
        return location;
    }

    public String getNamespace() {
        return namespace;
    }

    @Override
    public boolean equalsWith(Object object) {
        return equalsWith(object, this::sameImportDefinition);
    }

    public boolean sameImportDefinition(ImportDefinition other) {
        return sameModelDefinition(other)
                && same(importType, other.importType)
                && same(location, other.location)
                && same(namespace, other.namespace);
    }
}
