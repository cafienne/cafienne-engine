/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
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
}
