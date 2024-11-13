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

package com.casefabric.processtask.definition;

import com.casefabric.cmmn.definition.CMMNElementDefinition;
import com.casefabric.cmmn.definition.ModelDefinition;
import com.casefabric.cmmn.instance.task.process.ProcessTask;
import com.casefabric.processtask.implementation.InlineSubProcess;
import com.casefabric.processtask.implementation.SubProcess;
import com.casefabric.processtask.instance.ProcessTaskActor;
import org.w3c.dom.Element;

/**
 * Process Tasks in the engine can be implemented by extending {@link SubProcess}.
 * Sub process instances depend on their definition, which can be provided through the {@link InlineSubProcessDefinition}.
 * <br/>
 */
public abstract class InlineSubProcessDefinition extends SubProcessDefinition {
    protected InlineSubProcessDefinition(Element element, ModelDefinition processDefinition, CMMNElementDefinition parentElement) {
        super(element, processDefinition, parentElement);
    }

    /**
     * If the SubProcessDefinition can be run _within_ the Case, then override this method to return true.
     * Note, and inline SubProcessDefinition is run within the thread of the creation of the task in the case.
     * Also, the createInstance() method will be invoked with the Task as parameter, instead of the ProcessTaskActor.
     * This method returns false by default - every process runs within its own actor context.
     */
    public final boolean isInline() {
        return true;
    }

    public abstract InlineSubProcess<?> createInstance(ProcessTask task);

    @Override
    public SubProcess<?> createInstance(ProcessTaskActor processTaskActor) {
        return null;
    }
}
