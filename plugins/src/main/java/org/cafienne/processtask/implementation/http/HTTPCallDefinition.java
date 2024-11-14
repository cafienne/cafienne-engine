package org.cafienne.processtask.implementation.http;

import com.casefabric.cmmn.definition.CMMNElementDefinition;
import com.casefabric.cmmn.definition.ModelDefinition;
import org.w3c.dom.Element;

public class HTTPCallDefinition extends com.casefabric.processtask.implementation.http.HTTPCallDefinition {
    public HTTPCallDefinition(Element element, ModelDefinition processDefinition, CMMNElementDefinition parentElement) {
        super(element, processDefinition, parentElement);
    }
}
