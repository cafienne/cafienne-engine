package org.cafienne.processtask.implementation.report;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperReport;
import org.cafienne.cmmn.definition.CMMNElementDefinition;
import org.cafienne.cmmn.definition.Definition;
import org.cafienne.cmmn.instance.casefile.StringValue;
import org.cafienne.cmmn.instance.casefile.Value;
import org.cafienne.util.XMLHelper;
import org.w3c.dom.Element;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class JasperDefinition extends CMMNElementDefinition {
    final static String JASPER_XML_TAG = "jasperReport";

    private final boolean isStatic;
    private final String name;
    private String jasperDefinition = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
            "<jasperReport xmlns=\"http://jasperreports.sourceforge.net/jasperreports\" " +
            "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
            "xsi:schemaLocation=\"http://jasperreports.sourceforge.net/jasperreports http://jasperreports.sourceforge.net/xsd/jasperreport.xsd\" " +
            "name=\"EmptyReportDefinition\" pageWidth=\"500\" pageHeight=\"842\" columnWidth=\"500\" " +
            "leftMargin=\"0\" rightMargin=\"0\" topMargin=\"0\" bottomMargin=\"0\" " +
            "uuid=\"5a62986f-c97c-4e2f-b4ed-d9f38960dab4\" />";
    private JasperReport preCompiledReport = null;

    public JasperDefinition(Element element, Definition definition, CMMNElementDefinition parentElement) {
        super(element, definition, parentElement);

        this.name = parseAttribute("name", false);
        this.isStatic = this.name.isEmpty();
        if (isStatic) {
            Element jasperXml = XMLHelper.getElement(element, JASPER_XML_TAG);
            if (jasperXml != null) {
                jasperDefinition = XMLHelper.printXMLNode(jasperXml);
            }
            try {
                preCompiledReport = compile(jasperDefinition);
            } catch (JRException jre) {
                getDefinition().fatalError("Cannot compile the Jasper report definition", jre);
            }
        }
    }

    public JasperReport createInstance(PDFReport owner) throws JRException, MissingParameterException {
        if (isStatic) {
            return preCompiledReport;
        } else {
            Value<?> parameterValue = owner.getInputParameters().get(name);
            if (parameterValue instanceof StringValue) {
                return compile(((StringValue) parameterValue).getValue());
            } else {
                throw new MissingParameterException(this + " cannot be found in the task input parameters");
            }
        }
    }

    @Override
    public String toString() {
        return "Report definition '"+name+"'";
    }

    private JasperReport compile(String xml) throws JRException {
        InputStream is = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
        return JasperCompileManager.compileReport(is);
    }
}
