/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.processtask.definition;

import org.cafienne.cmmn.definition.CMMNElementDefinition;
import org.cafienne.cmmn.definition.ModelDefinition;
import org.cafienne.cmmn.instance.task.process.ProcessTask;
import org.cafienne.processtask.implementation.InlineSubProcess;
import org.cafienne.processtask.implementation.SubProcess;
import org.cafienne.processtask.instance.ProcessTaskActor;
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
     * Note, and inline SubProcessDefinition is ran within the thread of the creation of the task in the case.
     * Also, the createInstance() method will be invoked with the Task as parameter, instead of the ProcessTaskActor.
     * This method returns false by default - every process runs within it's own Akka Actor context.
     *
     * @return
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
