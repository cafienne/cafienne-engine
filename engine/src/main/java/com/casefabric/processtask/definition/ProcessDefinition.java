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
        this.subProcessDefinition = getCustomImplementation(SubProcessDefinition.class, true);
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

    @Override
    public boolean equalsWith(Object object) {
        return equalsWith(object, this::sameProcessDefinition);
    }

    public boolean sameProcessDefinition(ProcessDefinition other) {
        return sameModelDefinition(other)
                && same(subProcessDefinition, other.subProcessDefinition);
    }
}
