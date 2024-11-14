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

package com.casefabric.processtask.implementation.report;

import com.casefabric.cmmn.definition.CMMNElementDefinition;
import com.casefabric.cmmn.definition.ModelDefinition;
import com.casefabric.processtask.definition.SubProcessDefinition;
import com.casefabric.processtask.instance.ProcessTaskActor;
import org.w3c.dom.Element;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

public class PDFReportDefinition extends SubProcessDefinition {
    final static String PDF_REPORT_DATA = "pdfReportData";
    final static String REPORT_XML_TAG = "reportXml";

    final static String SUBREPORT_XML_TAG = "subReportXml";
    final static String REPORT_DATA_TAG = "reportData";


    private final JasperDefinition mainReportDefinition;
    private final Collection<JasperSubReportDefinition> subReportDefinitions = new ArrayList<>();
    private final ReportDataDefinition reportDataDefinition;

    public PDFReportDefinition(Element element, ModelDefinition processDefinition, CMMNElementDefinition parentElement) {
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
        return mainReportDefinition;
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

    @Override
    public boolean equalsWith(Object object) {
        return notYetImplemented();
    }
}
