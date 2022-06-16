/*
 * Copyright (C) 2022 Batav B.V. <https://www.batav.com/cafienne-enterprise>
 */

package org.cafienne.cmmn.expression.spel.api.process;

import org.cafienne.cmmn.expression.spel.api.ProcessActorRootObject;
import org.cafienne.processtask.instance.ProcessTaskActor;

public class InputMappingRoot extends ProcessActorRootObject {
    public InputMappingRoot(ProcessTaskActor model) {
        super(model);
        model.getMappedInputParameters().getValue().forEach(this::addProperty);
    }

    @Override
    public String getDescription() {
        return "mapping process input";
    }
}
