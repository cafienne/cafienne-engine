package org.cafienne.processtask.implementation.report;

import org.cafienne.cmmn.definition.CMMNElementDefinition;
import org.cafienne.cmmn.definition.ModelDefinition;
import org.w3c.dom.Element;

public class JasperSubReportDefinition extends JasperDefinition {
    private final String subReportName;

    public JasperSubReportDefinition(Element element, ModelDefinition modelDefinition, CMMNElementDefinition parentElement) {
        super(element, modelDefinition, parentElement);
        this.subReportName = parseAttribute("parameterName", true);
    }

    public String getSubReportName() {
        return subReportName;
    }

    @Override
    public String toString() {
        return "Sub report definition '" + getName() + "'";
    }
}
