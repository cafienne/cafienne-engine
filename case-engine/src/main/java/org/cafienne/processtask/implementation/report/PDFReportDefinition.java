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
import org.cafienne.processtask.definition.SubProcessDefinition;
import org.cafienne.processtask.instance.ProcessTaskActor;
import org.w3c.dom.Element;

import java.io.InputStream;
import java.util.*;

public class PDFReportDefinition extends SubProcessDefinition {
    final static String PDF_REPORT_DATA = "pdfReportData";
    final static String REPORT_XML_TAG = "reportXml";

    final static String SUBREPORT_XML_TAG = "subReportXml";
    final static String REPORT_DATA_TAG = "reportData";


    private final JasperDefinition mainReportDefinition;
    private final Collection<JasperSubReportDefinition> subReportDefinitions = new ArrayList();
    private final ReportDataDefinition reportDataDefinition;

    public PDFReportDefinition(Element element, Definition processDefinition, CMMNElementDefinition parentElement) {
        super(element, processDefinition, parentElement);
        mainReportDefinition = parse(REPORT_XML_TAG, JasperDefinition.class, true);
        parse(SUBREPORT_XML_TAG, JasperSubReportDefinition.class, subReportDefinitions);
        reportDataDefinition = parse(REPORT_DATA_TAG, ReportDataDefinition.class, false);
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

    JasperDefinition getReportDefinition() {
        return  mainReportDefinition;
    }

    Collection<JasperSubReportDefinition> getSubReportDefinitions() {
        return subReportDefinitions;
    }

    InputStream createDataStream(PDFReport report) {
        if (reportDataDefinition == null) { // Sometimes there is no report data. Then return a stream with an empty json.
            return ReportDataDefinition.EMPTY_STREAM;
        }
        return reportDataDefinition.createDataStream(report);
    }
}
