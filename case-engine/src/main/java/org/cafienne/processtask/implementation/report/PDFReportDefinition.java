/* 
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.processtask.implementation.report;

import org.cafienne.cmmn.definition.CMMNElementDefinition;
import org.cafienne.cmmn.definition.Definition;
import org.cafienne.cmmn.instance.casefile.Value;
import org.cafienne.cmmn.instance.casefile.ValueMap;
import org.cafienne.processtask.definition.SubProcessDefinition;
import org.cafienne.processtask.instance.ProcessTaskActor;
import org.cafienne.util.XMLHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class PDFReportDefinition extends SubProcessDefinition {
    private final static Logger logger = LoggerFactory.getLogger(PDFReportDefinition.class);

    final static String JASPER_XML_TAG = "jasperReport";
    final static String REPORT_XML_TAG = "reportXml";
    final static String SUBREPORT_XML_TAG = "subReportXml";
    final static String REPORT_DATA_TAG = "reportData";
    final static String PDF_REPORT_DATA = "pdfReportData";

    private final Map<String, InputStream> jasperSubReportXmls = new LinkedHashMap<>();
    private InputStream jasperReportXml;
    private InputStream jasperReportJsonData;

    public PDFReportDefinition(Element element, Definition processDefinition, CMMNElementDefinition parentElement) {
        super(element, processDefinition, parentElement);
    }

    @Override
    public PDFReport createInstance(ProcessTaskActor processTaskActor) {
        return new PDFReport(processTaskActor, this);
    }

    @Override
    public Set<String> getRawOutputParameterNames() {
        Set<String> pNames = super.getExceptionParameterNames();
        pNames.add(PDF_REPORT_DATA);
        return pNames;
    }

    InputStream getJasperReportXml() {
        return jasperReportXml;
    }

    InputStream getJasperReportJsonData() {
        return jasperReportJsonData;
    }

    Map<String, InputStream> getJasperSubReportXmls() {
        return jasperSubReportXmls;
    }

    void setInputParameters(ValueMap processInputParameters) {
        Element element = getElement();

        /**
         * Sets the report xml
         */
        Element reportXml = XMLHelper.getElement(element, REPORT_XML_TAG);
        if (reportXml == null)
            throw new RuntimeException(REPORT_XML_TAG + " tag is not found inside element " + getParentElement().getId());

        jasperReportXml = getJRXml(reportXml, processInputParameters);

        /**
         * Sets the sub-report xml(s)
         */
        Collection<Element> subReportXmls = XMLHelper.getChildrenWithTagName(element, SUBREPORT_XML_TAG);
        for (Element subReportXml : subReportXmls) {
            String reportParameter = subReportXml.getAttribute("parameterName");
            if (reportParameter == null)
                throw new RuntimeException(SUBREPORT_XML_TAG + " tag must have a parameterName attribute");

            jasperSubReportXmls.put(reportParameter, getJRXml(subReportXml, processInputParameters));
        }

        /**
         * Sets the report data
         */
        Element reportData = XMLHelper.getElement(element, REPORT_DATA_TAG);
        String paramName = REPORT_DATA_TAG;
        if (reportData != null) {
            paramName = reportData.getAttribute("name");
        }

        jasperReportJsonData = getReportInputData(paramName, processInputParameters);
    }

    private InputStream getJRXml(Element report, ValueMap processInputParameters) {
        String reportName = report.getAttribute("name");
        String jrXmlString = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<jasperReport xmlns=\"http://jasperreports.sourceforge.net/jasperreports\" " +
                "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
                "xsi:schemaLocation=\"http://jasperreports.sourceforge.net/jasperreports http://jasperreports.sourceforge.net/xsd/jasperreport.xsd\" " +
                "name=\"JsonOrdersReport\" pageWidth=\"500\" pageHeight=\"842\" columnWidth=\"500\" " +
                "leftMargin=\"0\" rightMargin=\"0\" topMargin=\"0\" bottomMargin=\"0\" " +
                "uuid=\"5a62986f-c97c-4e2f-b4ed-d9f38960dab4\" />";

        if (reportName == null || reportName.isEmpty()) {
            Element jasperXml = XMLHelper.getElement(report, JASPER_XML_TAG);
            if (jasperXml != null) {
                try {
                    TransformerFactory transFactory = TransformerFactory.newInstance();
                    Transformer transformer = transFactory.newTransformer();
                    StringWriter buffer = new StringWriter();
                    transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
                    transformer.transform(new DOMSource(jasperXml), new StreamResult(buffer));
                    jrXmlString = buffer.toString();
                } catch (TransformerException e) {
                    logger.error("Error while transforming jasper report xml", e);
                    throw new RuntimeException("Error while transforming jasper report xml", e);
                }
            }
        } else {
            Value<?> jrXml = processInputParameters.get(reportName);
            if (jrXml != Value.NULL) jrXmlString = (String) jrXml.getValue();
        }

        return new ByteArrayInputStream(jrXmlString.getBytes());
    }

    InputStream getReportInputData(String reportDataName, ValueMap processInputParameters) {
        String jsonDataString = "{}";
        Value<?> jsonData = processInputParameters.get(reportDataName);
        if (jsonData != Value.NULL) jsonDataString = jsonData.getValue().toString();


        return new ByteArrayInputStream(jsonDataString.getBytes());
    }
}
