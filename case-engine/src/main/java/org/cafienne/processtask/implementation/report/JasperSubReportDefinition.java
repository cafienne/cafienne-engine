package org.cafienne.processtask.implementation.report;

import org.cafienne.cmmn.definition.CMMNElementDefinition;
import org.cafienne.cmmn.definition.Definition;
import org.w3c.dom.Element;

public class JasperSubReportDefinition extends JasperDefinition {
    private final String subReportName;

    public JasperSubReportDefinition(Element element, Definition definition, CMMNElementDefinition parentElement) {
        super(element, definition, parentElement);
        this.subReportName = parseAttribute("parameterName", true);
    }

    public String getSubReportName() {
        return subReportName;
    }

    @Override
    public String toString() {
        return "Sub report definition '"+getName()+"'";
    }
}
