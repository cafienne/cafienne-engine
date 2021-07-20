/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.processtask.definition;

import org.cafienne.cmmn.definition.DefinitionsDocument;
import org.cafienne.cmmn.definition.ModelDefinition;
import org.cafienne.cmmn.definition.task.TaskImplementationContract;
import org.w3c.dom.Element;

/**
 * Parsed structure of a process definition (a top-level &lt;process&gt; element within a &lt;definitions&gt; document).
 * The engine assumes there is a &lt;implementation&gt; tag inside the process, holding a <code>class</code> attribute.
 * This class attribute is interpreted as a class name for a class that implements the {@link SubProcessDefinition} interface.
 */
public class ProcessDefinition extends ModelDefinition implements TaskImplementationContract {
    private final SubProcessDefinition subProcessDefinition;

    public ProcessDefinition(Element element, DefinitionsDocument document) {
        super(element, document);
        this.subProcessDefinition = getExtension("implementation", SubProcessDefinition.class, true);
    }

    /**
     * Returns the custom, non-CMMN implementation of the process, used to instantiate the sub process when it needs to be instantiated.
     *
     * @return
     */
    public SubProcessDefinition getImplementation() {
        return subProcessDefinition;
    }

    public InlineSubProcessDefinition getInlineImplementation() {
        return (InlineSubProcessDefinition) subProcessDefinition;
    }
}
